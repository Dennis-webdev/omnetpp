package org.omnetpp.inifile.editor.model;

import static org.omnetpp.inifile.editor.model.ConfigRegistry.CFGID_NETWORK;
import static org.omnetpp.inifile.editor.model.ConfigRegistry.EXTENDS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.omnetpp.common.ui.HoverSupport;
import org.omnetpp.common.util.StringUtils;
import org.omnetpp.inifile.editor.model.InifileAnalyzer.KeyType;
import org.omnetpp.ned.model.pojo.ParamElement;
import org.omnetpp.ned.model.pojo.SubmoduleElement;

/**
 * Produces hover information for various Inifile parts.
 * 
 * @author Andras
 */
public class InifileHoverUtils {
	/**
	 * Generates tooltip for an inifile section.
	 * @param section  null is accepted
	 */
	public static String getSectionHoverText(String section, IInifileDocument doc, InifileAnalyzer analyzer, boolean allProblems) {
		if (section == null || !doc.containsSection(section))
			return null;

		// problem markers: display the section header's and extends's errors as text,
		// then display how many more there are within the section  
		IMarker[] markers1 = InifileUtils.getProblemMarkersFor(section, null, doc);
		IMarker[] markers2 = InifileUtils.getProblemMarkersFor(section, EXTENDS, doc);
		IMarker[] allMarkers = InifileUtils.getProblemMarkersForWholeSection(section, doc);
		List<IMarker> markersDisplayed = new ArrayList<IMarker>();
		markersDisplayed.addAll(Arrays.asList(markers1));
		markersDisplayed.addAll(Arrays.asList(markers2));

		int numErrors = 0, numWarnings = 0;
		for (IMarker m : allMarkers) {
			if (!markersDisplayed.contains(m)) {
				switch (m.getAttribute(IMarker.SEVERITY, -1)) {
				case IMarker.SEVERITY_ERROR: numErrors++; break;
				case IMarker.SEVERITY_WARNING: numWarnings++; break;
				}
			}
		}

		String numErrorsText = "";
		if (numErrors+numWarnings > 0) {
			if (numErrors>0 && numWarnings>0)
				numErrorsText += numErrors + " error(s) and " + numWarnings + " warning(s)"; 
			else if (numErrors>0)
				numErrorsText += numErrors + " error(s)"; 
			else if (numWarnings>0)
				numErrorsText += numWarnings + " warning(s)";
			if (markersDisplayed.size() > 0)
				numErrorsText += " more";
			numErrorsText += " in the section body";

			numErrorsText = "<i>" + numErrorsText + "</i><br><br>";
		}

		String text = getProblemsHoverText(markersDisplayed.toArray(new IMarker[]{}), false) + numErrorsText;
		
		// name and description
		text += "<b>"+section+"</b>";
		String description = doc.getValue(section, "description");
		if (description != null)
			text += " -- " + description;
		text += "<br>\n";

		// section chain
		String[] sectionChain = InifileUtils.resolveSectionChain(doc, section);
		if (sectionChain.length >= 2)
			text += "Fallback order: " + StringUtils.join(sectionChain, " &gt; ") + " <br>\n"; //XXX decide terminology: "Lookup order" or "Section fallback chain" ? also: "," or ">" ?

		// network
		String networkName = InifileUtils.lookupConfig(sectionChain, CFGID_NETWORK.getKey(), doc);
		text += "Network: " + (networkName==null ? "not set" : networkName) + " <br>\n";

		// unassigned parameters
		if (analyzer != null) {
			ParamResolution[] resList = analyzer.getUnassignedParams(section);
			if (resList.length==0) {
				if (networkName != null)
					text += "<br>\nNo unassigned NED parameters in this section.";
			}
			else {
				text += "<br>\nThis section does not seem to assign the following NED parameters:\n<ul>";
				for (ParamResolution res : resList)
					text += " <li>" + res.moduleFullPath + "." +res.paramDeclNode.getName() + "</li>\n";
				text += "</ul>";
			}
		}
		return HoverSupport.addHTMLStyleSheet(text);
	}

	/**
	 * Generate tooltip for an inifile entry.
	 * @param section  null is accepted
	 * @param key      null is accepted
	 */
	//XXX should tolerate analyzer==null
	public static String getEntryHoverText(String section, String key, IInifileDocument doc, InifileAnalyzer analyzer) {
		if (section == null || key == null || !doc.containsKey(section, key))
			return null;

		KeyType keyType = (key == null) ? KeyType.CONFIG : InifileAnalyzer.getKeyType(key);
		if (keyType==KeyType.CONFIG) {
			// config key: display description
			return getConfigHoverText(section, key, doc);
		}
		else if (keyType == KeyType.PARAM || key.endsWith(".apply-default")) { //XXX hardcoded key name
			// parameter assignment: display which parameters it matches
			return getParamKeyHoverText(section, key, analyzer);
		}
		else if (keyType == KeyType.PER_OBJECT_CONFIG) {
			return null; // TODO display which modules it applies to, plus comment maybe?
		}
		else {
			return null; // should not happen (invalid key type)
		}
	}

	/**
	 * Generates tooltip for a config entry.
	 */
	public static String getConfigHoverText(String section, String key, IInifileDocument doc) {
		IMarker[] markers = InifileUtils.getProblemMarkersFor(section, key, doc);
		String text = getProblemsHoverText(markers, false);
		ConfigKey entry = ConfigRegistry.getEntry(key);
		if (entry==null)
		    entry = ConfigRegistry.getPerObjectEntry(key.replaceFirst("^.*\\.", ""));
		if (entry == null)
			return HoverSupport.addHTMLStyleSheet(text);

		text += "<b>[General]"+(entry.isGlobal() ? "" : " or [Config X]")+" / ";
		text += (entry.isPerObject() ? "**." : "") + entry.getKey();
		text += " = &lt;" + entry.getDataType().name().replaceFirst("CFG_", "");
		if (entry.getDefaultValue()!=null && !entry.getDefaultValue().equals(""))
			text += ", default: " + entry.getDefaultValue();
		if (entry.getUnit()!=null)
			text += ", unit: "+entry.getUnit();
		text += "&gt;</b><br>\n<br>\n";
		text += htmlizeDescription(entry.getDescription()) + "<br>\n";

		return HoverSupport.addHTMLStyleSheet(text);
	}

	protected static String htmlizeDescription(String text) {
	    text = text.replace("<", "&lt;");
	    text = text.replace(">", "&gt;");
        text = text.replaceAll("\n( *)    ", "\n$1&nbsp;&nbsp;&nbsp;&nbsp;");
        text = text.replaceAll("\n( *)  ", "\n$1&nbsp;&nbsp;");
        text = text.replaceAll("\n ", "\n&nbsp;");
	    text = text.replace("\n", "<p>");
	    return text;
	}
	
	/**
	 * Generate tooltip for a param key entry
	 */
	public static String getParamKeyHoverText(String section, String key, InifileAnalyzer analyzer) {
		IMarker[] markers = InifileUtils.getProblemMarkersFor(section, key, analyzer.getDocument());
		String text = getProblemsHoverText(markers, false);
		text += "<b>[" + section + "] / " + key + "</b><br>\n";
		ParamResolution[] resList = analyzer.getParamResolutionsForKey(section, key);
		if (resList.length==0) {
			text += "<br>Does not match any module parameters.";
			return HoverSupport.addHTMLStyleSheet(text);
		}

		text += "<br><b>Applies to:</b><br>\n";
		String[] allSectionNames = analyzer.getDocument().getSectionNames();
        text += formatParamResolutions(resList, allSectionNames, false);
        text += "<br><b>Details:</b><br>\n";
        text += formatParamResolutions(resList, allSectionNames, true);
		return HoverSupport.addHTMLStyleSheet(text);
	}

    private static String formatParamResolutions(ParamResolution[] resList, String[] allSectionNames, boolean details) {
        String text = "";
        
        // Build a map about sections and parameter declarations:
        // - key: section name
        // - value: the parameter declarations that this config key matches in the given section
        //
        // Example: we're processing the param resolutions of **.mac.address; 
        // the resulting map will contain:
        //   [General] => {EtherMac.address, Ieee80211Mac.address}
        //   [Config Foo] => {EtherMac.address, Ieee80211Mac.address}
        //   [Config Bar] => {Ieee80211Mac.address}
        //
        Map<String,Set<ParamElement>> sectionParamDecls = new LinkedHashMap<String, Set<ParamElement>>();
        for (ParamResolution res : resList) {
            String sectionName = res.activeSection;
            if (!sectionParamDecls.containsKey(sectionName))
                sectionParamDecls.put(sectionName, new HashSet<ParamElement>());
            sectionParamDecls.get(sectionName).add(res.paramDeclNode);
        }

        // Now: it is typical that the config key matches the same parameter declarations 
        // in all sections, so we want to group them. This is what we want to see:
        //   {EtherMac.address, Ieee80211Mac.address} => {[General], [Config Foo]}
        //   {Ieee80211Mac.address} => {[Config Bar]}
        //
        Map<Set<ParamElement>, Set<String>> map2 = new LinkedHashMap<Set<ParamElement>, Set<String>>();
        for (String sectionName : sectionParamDecls.keySet()) {
            Set<ParamElement> declSet = sectionParamDecls.get(sectionName);
            if (!map2.containsKey(declSet)) 
                map2.put(declSet, new LinkedHashSet<String>());
            map2.get(declSet).add(sectionName);
        }
                
        // Now we turn this into HTML
        for (Set<ParamElement> paramDecls : map2.keySet()) {
            Set<String> sectionNames = map2.get(paramDecls);

            // print section name(s), unless there'a only one section
            if (sectionNames.size() == allSectionNames.length) {
                if (allSectionNames.length > 1)
                    text += "&nbsp;All sections";
            }
            else
                text += "&nbsp;[" + StringUtils.join(sectionNames, "], [") + "]\n";
            
            // and the param declarations
            text += "<ul>";
            for (ParamElement paramDeclNode : paramDecls) {
                String paramName = paramDeclNode.getName();
                String paramValue = paramDeclNode.getValue();
                if (paramDeclNode.getIsDefault())
                    paramValue = "default("+paramValue+")";
                String optParamValue = StringUtils.isEmpty(paramValue) ? "" : " = " + paramValue;
                String paramType = paramDeclNode.getAttribute(ParamElement.ATT_TYPE);
                if (paramDeclNode.getIsVolatile())
                    paramType = "volatile " + paramType;
                String paramDeclaredOn = paramDeclNode.getEnclosingTypeElement().getName();
                String comment = StringUtils.makeBriefDocu(paramDeclNode.getComment(), 250);
                String optComment = comment==null ? "" : ("<br><i>\"" + comment + "\"</i>");

                // print parameter declaration 
                text += "<li>"+paramDeclaredOn + ": " + paramType + " " + paramName + optParamValue + (details ? "" : optComment) + "\n";
                if (details) {
                    text += "<ul>\n";
                    for (String sectionName : sectionNames)
                        for (ParamResolution res : resList)
                            if (res.paramDeclNode == paramDeclNode && res.activeSection.equals(sectionName))
                                text += " <li><i>" + res.moduleFullPath + "." + paramName + "</i>" +
                                		(sectionNames.size()==1 ? "" : " [" + sectionName + "]") + "</li>\n";
                    text += "</ul>";
                }
                text += "</li>\n";
            }
            text += "</ul>";
        }
        return text;
    }

	public static String getProblemsHoverText(IMarker[] markers, boolean lineNumbers) {
		if (markers.length==0) 
			return "";
		
		String text = "";
		for (IMarker marker : markers) {
			String severity = "";
			switch (marker.getAttribute(IMarker.SEVERITY, -1)) {
				case IMarker.SEVERITY_ERROR: severity = "Error"; break;
				case IMarker.SEVERITY_WARNING: severity = "Warning"; break;
				case IMarker.SEVERITY_INFO: severity = "Info"; break;
			}
			String lineNumber = lineNumbers ? "Line "+marker.getAttribute(IMarker.LINE_NUMBER, -1)+": " : "";  
			text += "<i>"+lineNumber+severity+": " + marker.getAttribute(IMarker.MESSAGE, "") + "</i><br/>\n";
		}
		return text+"<br/>";
	}
	
	/**
	 * Generate tooltip for a NED parameter
	 */
	public static String getParamHoverText(SubmoduleElement[] pathModules, ParamElement paramDeclNode, ParamElement paramValueNode) {
		String paramName = paramDeclNode.getName();
		
		String paramType = paramDeclNode.getAttribute(ParamElement.ATT_TYPE);
		String paramDeclaredOn = paramDeclNode.getSelfOrEnclosingTypeElement().getName();
		String comment = StringUtils.makeBriefDocu(paramDeclNode.getComment(), 60);
		String optComment = comment==null ? "" : (" -- \"" + comment + "\"");

		String text = ""; //TODO
		text += paramDeclaredOn + "." + paramName + " : "+ paramType + optComment + "\n";
		//XXX more info.....
		return HoverSupport.addHTMLStyleSheet(text);
	}
	
}
