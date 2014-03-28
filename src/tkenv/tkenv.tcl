#=================================================================
#  TKENV.TCL - part of
#
#                     OMNeT++/OMNEST
#            Discrete System Simulation in C++
#
#=================================================================

#----------------------------------------------------------------#
#  Copyright (C) 1992-2008 Andras Varga
#
#  This file is distributed WITHOUT ANY WARRANTY. See the file
#  `license' for details on this and other legal matters.
#----------------------------------------------------------------#

#
# Load library files
#
if [info exist OMNETPP_TKENV_DIR] {

   set dir $OMNETPP_TKENV_DIR

   source [file join $dir animate.tcl]
   source [file join $dir animate2.tcl]
   source [file join $dir balloon.tcl]
   source [file join $dir canvaslabel.tcl]
   source [file join $dir dialogs.tcl]
   source [file join $dir fieldspage.tcl]
   source [file join $dir gateinspector.tcl]
   source [file join $dir genericobjectinspector.tcl]
   source [file join $dir histograminspector.tcl]
   source [file join $dir icons.tcl]
   source [file join $dir inspector.tcl]
   source [file join $dir inspectorlist.tcl]
   source [file join $dir inspectorlistbox.tcl]
   source [file join $dir inspectorutil.tcl]
   source [file join $dir layouterenv.tcl]
   source [file join $dir loginspector.tcl]
   source [file join $dir main.tcl]
   source [file join $dir menuproc.tcl]
   source [file join $dir moduleinspector.tcl]
   source [file join $dir moduleinspectorutil.tcl]
   source [file join $dir objecttree.tcl]
   source [file join $dir outputvectorinspector.tcl]
   source [file join $dir timeline.tcl]
   source [file join $dir tkenvrc.tcl]
   source [file join $dir toolbutton.tcl]
   source [file join $dir tree.tcl]
   source [file join $dir watchinspector.tcl]
   source [file join $dir widgets.tcl]
   source [file join $dir windows.tcl]
   source [file join $dir clearlooksimg.tcl]
   source [file join $dir clearlooks.tcl]
}

#
# Execute startup code
#
proc startTkenv {} {
  global OMNETPP_IMAGE_PATH
  global OMNETPP_PLUGIN_PATH

  wm withdraw .
  checkTclTkVersion
  setupTkOptions
  genericBindings
  initBalloons
  createOmnetppWindow
  puts ""
  loadBitmaps $OMNETPP_IMAGE_PATH
  loadPlugins $OMNETPP_PLUGIN_PATH
  loadTkenvrc "~/.tkenvrc"
  loadTkenvrc ".tkenvrc"
  reflectSettingsInGui
  determineClocksPerSec

  global tcl_platform
  if {$tcl_platform(platform) == "windows"} {
      # without "update", the main window comes up on top of all others, it also
      # obscures any dialog box (e.g. param prompt) that might appear at startup!
      update
  }

}

