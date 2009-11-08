package org.omnetpp.common.wizard;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.omnetpp.common.CommonPlugin;
import org.omnetpp.common.json.ExceptionErrorListener;
import org.omnetpp.common.json.JSONValidatingReader;

import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;

/**
 * Project template loaded from a workspace project.
 * @author Andras
 */
//FIXME: freemarker logging currently goes to stdout
public class WorkspaceBasedContentTemplate extends ContentTemplate {
	public static final String TEMPLATE_PROPERTIES_FILENAME = "template.properties";
	public static final Image MISSING_IMAGE = ImageDescriptor.getMissingImageDescriptor().createImage();
	
	// property names:
	public static final String PROP_TEMPLATENAME = "templateName"; // template display name
	public static final String PROP_SUPPORTEDWIZARDTYPES = "supportedWizardTypes"; // list of items: "project", "simulation", "nedfile", "inifile", "network", etc
	public static final String PROP_TEMPLATEDESCRIPTION = "templateDescription"; // template description
	public static final String PROP_TEMPLATECATEGORY = "templateCategory"; // template category (parent tree node)
	public static final String PROP_TEMPLATEIMAGE = "templateImage"; // template icon name
	public static final String PROP_IGNORERESOURCES = "ignoreResources"; // list of files NOT top copy into dest folder; basic glob patterns accepted
	public static final String PROP_VERBATIMFILES = "verbatimFiles"; // list of files to copy verbatim, even if they would be ignored otherwise; basic glob patterns accepted

	protected IFolder templateFolder;
	protected Properties properties = new Properties();
	protected Set<String> supportedWizardTypes = new HashSet<String>();
	protected List<String> ignoreResourcePatterns = new ArrayList<String>();
	protected List<String> verbatimFilePatterns = new ArrayList<String>();
    private boolean imageAlreadyLoaded = false;

    /**
     * Currently only tests whether the given folder contains a template.properties file.
     */
    public static boolean looksLikeTemplateFolder(IFolder folder) {
        return folder.getFile(TEMPLATE_PROPERTIES_FILENAME).exists();
    }
    
    /**
     * Loads the template from the given folder. This is a relatively cheap operation
     * (only the template.properties file is read), so it is OK for the wizard to 
     * instantiate WorkspaceBasedContentTemplate for each candidate folder just to determine
     * whether it should be offered to th user.
     */
	public WorkspaceBasedContentTemplate(IFolder folder) throws CoreException {
		super();
		this.templateFolder = folder;

		properties = loadProperties(folder);

		// note: image will be loaded lazily, in getImage()
		setName(StringUtils.defaultIfEmpty(properties.getProperty(PROP_TEMPLATENAME), folder.getName()));
		setDescription(StringUtils.defaultIfEmpty(properties.getProperty(PROP_TEMPLATEDESCRIPTION), "Template loaded from " + folder.getFullPath()));
		setCategory(StringUtils.defaultIfEmpty(properties.getProperty(PROP_TEMPLATECATEGORY), folder.getProject().getName()));

		ignoreResourcePatterns.add("**/*.xswt");
		ignoreResourcePatterns.add("**/*.fti");  // note: "*.ftl" is NOT to be added! (or they'd be skipped altogether)
		ignoreResourcePatterns.add("**/*.jar");
		ignoreResourcePatterns.add(TEMPLATE_PROPERTIES_FILENAME);

		// the following options may not be modified via the wizard, so they are initialized here
        String[] labels = SWTDataUtil.toStringArray(StringUtils.defaultString(properties.getProperty(PROP_SUPPORTEDWIZARDTYPES))," *, *");
        supportedWizardTypes.addAll(Arrays.asList(labels));
		
		for (String item : SWTDataUtil.toStringArray(StringUtils.defaultString(properties.getProperty(PROP_IGNORERESOURCES))," *, *"))
		    ignoreResourcePatterns.add(item);

		for (String item : SWTDataUtil.toStringArray(StringUtils.defaultString(properties.getProperty(PROP_VERBATIMFILES))," *, *"))
		    verbatimFilePatterns.add(item);
	}

    /**
     * Returns the workspace folder from which the template was loaded.
     */
    public IFolder getTemplateFolder() {
        return templateFolder;
    }
    
    /**
     * Returns the values in the supportedWizardTypes property file entry.
     */
    public Set<String> getSupportedWizardTypes() {
        return supportedWizardTypes;
    }

    @Override
	public Image getImage() {
	    // lazy loading of the image
	    if (!imageAlreadyLoaded) {
	        imageAlreadyLoaded = true;
	        String imageFileName = properties.getProperty(PROP_TEMPLATEIMAGE);
	        if (imageFileName != null) {
	            ignoreResourcePatterns.add(imageFileName);
	            IFile file = templateFolder.getFile(new Path(imageFileName));
	            IPath locPath = file.getLocation();
	            String loc = locPath==null ? "<unknown>" : locPath.toOSString();
	            ImageRegistry imageRegistry = CommonPlugin.getDefault().getImageRegistry();
	            Image image = imageRegistry.get(loc);
	            if (image==null) {
	                try {
	                    image = new Image(Display.getDefault(), loc);
	                    imageRegistry.put(loc, image);
	                } catch (SWTException e) {
	                    CommonPlugin.logError("Error loading image for project template in "+templateFolder.getFullPath(), e);
	                    image = MISSING_IMAGE;
	                }
	            }
	            setImage(image);
	        }
	    }
        return super.getImage();
	}

    protected static Properties loadProperties(IFolder folder) throws CoreException {
        InputStream is = null;
        try {
            Properties result = new Properties();
			is = folder.getFile(TEMPLATE_PROPERTIES_FILENAME).getContents();
			result.load(is);
			is.close();
			return result;
		} 
        catch (IOException e) {
		    try { is.close(); } catch (IOException e2) { }
			throw CommonPlugin.wrapIntoCoreException(e);
		}
    }

	@Override
	public CreationContext createContext(IContainer folder) {
		CreationContext context = super.createContext(folder);

		// default values of recognized options (will be overwritten from property file)
		context.getVariables().put(PROP_IGNORERESOURCES, "");
		
		// add property file entries as template variables
		for (Object key : properties.keySet()) {
			Object value = properties.get(key);
			Assert.isTrue(key instanceof String && value instanceof String);
				context.getVariables().put((String)key, parseJSON((String)value));
		}
		
		// add more predefined variables (these ones cannot be overwritten from the property file, would make no sense)
		context.getVariables().put("templateFolderName", templateFolder.getName());
		context.getVariables().put("templateFolderPath", templateFolder.getFullPath().toString());
		context.getVariables().put("templateProject", templateFolder.getProject().getName());

		return context;
	}
	
	/**
	 * Overridden so that we can load JAR files from the template folder and the project's "plugins" folder
	 */
	@Override
	protected ClassLoader createClassLoader() {
	    List<URL> urls = new ArrayList<URL>();
	    try {
	        // load from the template folder and from the project's "plugins" folder
	        IContainer[] folders = new IContainer[] { templateFolder, templateFolder.getProject().getFolder(new Path("plugins")) };
	        for (IContainer folder : folders)
	            for (IResource resource : folder.members())
	                if (resource instanceof IFile && resource.getFileExtension().equals("jar"))
	                    urls.add(new URL("file", "", resource.getLocation().toPortableString()));
	    } 
	    catch (Exception e) {
	        CommonPlugin.logError("Error assembling classpath for loading jars from the workspace", e);
	    }
	    return new URLClassLoader(urls.toArray(new URL[]{}), getClass().getClassLoader());
	}
	
	@Override
	protected Configuration createFreemarkerConfiguration() {
	    // add workspace template loader
	    Configuration cfg = super.createFreemarkerConfiguration();
	    cfg.setTemplateLoader(new MultiTemplateLoader( new TemplateLoader[] { 
	            cfg.getTemplateLoader(), 
	            new WorkspaceTemplateLoader(templateFolder)
	    }));
	    return cfg;
	}

	public ICustomWizardPage[] createCustomPages() throws CoreException {
    	// collect page IDs from property file ("page.1", "page.2" etc keys)
    	int[] pageIDs = new int[0];
    	for (Object key : properties.keySet())
    		if (((String)key).matches("page\\.[0-9]+\\.file"))
    			pageIDs = ArrayUtils.add(pageIDs, Integer.parseInt(((String)key).replaceFirst("^page\\.([0-9]+)\\.file$", "$1")));
    	Arrays.sort(pageIDs);
    	
    	// create pages
    	ICustomWizardPage[] result = new ICustomWizardPage[pageIDs.length];
		for (int i = 0; i < pageIDs.length; i++) {
			// create page
			int pageID = pageIDs[i];
			String xswtFileName = properties.getProperty("page."+pageID+".file");
			String condition = properties.getProperty("page."+pageID+".condition");
			IFile xswtFile = templateFolder.getFile(new Path(xswtFileName));
			if (!xswtFile.exists())
				throw new CoreException(new Status(IStatus.ERROR, CommonPlugin.PLUGIN_ID, "Template file not found: "+xswtFile.getFullPath()));
			try {
                result[i] = new XSWTWizardPage(this, getName()+"#"+pageID, xswtFile, condition);
            }
            catch (IOException e) {
                throw new CoreException(new Status(IStatus.ERROR, CommonPlugin.PLUGIN_ID, "Error loading template file "+xswtFile.getFullPath()));
            }

			// set title and description
			String title = StringUtils.defaultIfEmpty(properties.getProperty("page."+pageID+".title"), getName());
			String description = StringUtils.defaultIfEmpty(properties.getProperty("page."+pageID+".description"),"Select options below"); // "1 of 5" not good as default, because of conditional pages
			result[i].setTitle(title);
			result[i].setDescription(description);
		}
		
		return result;
    }

	/**
	 * Parse text as JSON. Returns Boolean, String, Number, List or Map. If the
	 * text does not look like JSON, treats it as an unquoted literal string, and
	 * returns it as String.
	 *  
	 * @throws IllegalArgumentException on JSON parse error.
	 */
	public static Object parseJSON(String text) {
		String numberRegex = "\\s*[+-]?[0-9.]+([eE][+-]?[0-9]+)?\\s*"; // sort of
		if (text.equals("true") || text.equals("false") || text.matches(numberRegex) || 
				text.trim().startsWith("[") || text.trim().startsWith("{")) {
			// looks like JSON -- parse as such
			JSONValidatingReader reader = new JSONValidatingReader(new ExceptionErrorListener());
			return reader.read(text); // throws IllegalArgumentException on parse errors
		} 
		else {
			// apparently not JSON -- take it as a literal string with missing quotes
			return text.trim();
		}
	}
	
	public void performFinish(CreationContext context) throws CoreException {
		substituteNestedVariables(context);

		// copy over files and folders, with template substitution
		copy(templateFolder, context.getFolder(), context); 
	}
	
	protected void copy(IFolder folder, IContainer destFolder, CreationContext context) throws CoreException {
	    for (IResource resource : folder.members()) {
	        IPath relativePath = resource.getFullPath().removeFirstSegments(templateFolder.getFullPath().segmentCount());
	        if (resource instanceof IFolder && !matchesAny(relativePath.toString(), ignoreResourcePatterns)) {
	            // create
	            IFolder subfolder = (IFolder)resource;
	            IFolder newSubfolder = destFolder.getFolder(new Path(subfolder.getName()));
	            if (!newSubfolder.exists())
	                newSubfolder.create(true, true, context.getProgressMonitor());
	            copy(subfolder, newSubfolder, context);
	        }
	        if (resource instanceof IFile) {
	            IFile file = (IFile)resource;
	            boolean isFtlFile = file.getFileExtension().equals("ftl");
                if (matchesAny(relativePath.toString(), verbatimFilePatterns) || (!isFtlFile && !matchesAny(relativePath.toString(), ignoreResourcePatterns))) {
	                // copy it verbatim
	                IFile newFile = destFolder.getFile(new Path(file.getName()));
	                if (newFile.exists())
	                    newFile.delete(true, context.getProgressMonitor());
	                file.copy(newFile.getFullPath(), true, context.getProgressMonitor());
	            } 
                else if (isFtlFile && !matchesAny(relativePath.toString(), ignoreResourcePatterns)) {
                    // copy it with template substitution
                    IFile newFile = destFolder.getFile(new Path(file.getName()).removeFileExtension());
                    createFileFromWorkspaceResource(newFile, relativePath.toString(), context);
                }
	        }
	    }
	}

    protected void createFileFromWorkspaceResource(IFile file, String templateName, CreationContext context) throws CoreException {
		createFile(file, getFreemarkerConfiguration(), templateName, context);
    }

    /**
     * Returns whether the given file name matches any of the given glob patterns.
     * Only a limited subset of glob patterns is recognized: '*' and '?' only, 
     * no curly braces or square brackets. Extra: "**" is recognized.
     * Note: "*.txt" means text files in the ROOT folder. To mean "*.txt in any 
     * folder", specify "** / *.txt" (without the spaces).
     */
    protected static boolean matchesAny(String path, List<String> basicGlobPatterns) {
        path = path.replace('\\', '/'); // simplify matching
        for (String pattern : basicGlobPatterns) {
            String regex = "^" + pattern.replace(".", "\\.").replace("?", ".").replace("**/", "(.&/)?").replace("**", ".&").replace("*", "[^/]*").replace(".&", ".*") + "$";
            if (path.matches(regex))
                return true;
        }
        return false;
    }
}
