#==========================================================================
#  DISPSTR.TCL -
#            part of the GNED, the Tcl/Tk graphical topology editor of
#                            OMNeT++
#
#   By Andras Varga
#
#==========================================================================

#----------------------------------------------------------------#
#  Copyright (C) 1992-2001 Andras Varga
#  Technical University of Budapest, Dept. of Telecommunications,
#  Stoczek u.2, H-1111 Budapest, Hungary.
#
#  This file is distributed WITHOUT ANY WARRANTY. See the file
#  `license' for details on this and other legal matters.
#----------------------------------------------------------------#


# split_dispstr --
#
# Split up display string into an array.
#    dispstr:         display string
#    tagsarrayname:   dest array name
#    returns:         original order of tags (as a list)
#
# Example:
#   if "p=50,99;i=cloud" is parsed into array 'a', the result is:
#      $a(p) = {50 99}
#      $a(i) = {cloud}
#
proc split_dispstr {dispstr tagsarrayname} {
   upvar $tagsarrayname tags

   set tagorder {}
   foreach tag [split $dispstr {;}] {
      set tag [split $tag {=}]
      set key [lindex $tag 0]
      set val [split [lindex $tag 1] {,}]

      lappend tagorder $key
      if {$key != ""} {
         set tags($key) $val
      }
   }
   return $tagorder
}


# assemble_dispstr --
#
# Assemble and return a display string from a form produced by split_dispstr
#    tagsarrayname: src array name
#    tagorder:      preferred order of tags (as a list)
#    returns:       display string
#
proc assemble_dispstr {tagsarrayname tagorder} {
   upvar $tagsarrayname tags

   set dispstr ""
   # loop through all tags in their preferred order
   foreach tag [lsort -command [list _dispstr_ordertags $tagorder] [array names tags]] {
       set vals $tags($tag)
       # discard empty elements at end of list
       while {$vals!="" && [lindex $vals end]==""} {
           set vals [lreplace $vals end end]
       }
       if {$vals!=""} {
           append dispstr "$tag="
           append dispstr [join $vals ","]
           append dispstr ";"
       }
   }

   # cut off trailing ';'
   if {$dispstr!=""} {
       set dispstr [string range $dispstr 0 [expr [string length $dispstr]-2]]
   }
   return $dispstr
}

# private proc for assemble_dispstr
proc _dispstr_ordertags {order t1 t2} {
   return [expr [lsearch -exact $order $t1] - [lsearch -exact $order $t2]]
}


# parse_module_dispstr --
#
# update a 'module' ned element with values from its display string
#
proc parse_module_dispstr {key} {
   global ned

   split_dispstr $ned($key,displaystring) tags

   # GNED currently only handles only few values from a dispstr...
   if [info exist tags(p)] {
      set ned($key,disp-xpos) [lindex $tags(p) 0]
      set ned($key,disp-ypos) [lindex $tags(p) 1]
   }
   if [info exist tags(b)] {
      set ned($key,disp-xsize) [lindex $tags(b) 0]
      set ned($key,disp-ysize) [lindex $tags(b) 1]
   }
   if [info exist tags(o)] {
      set ned($key,disp-fillcolor) [lindex $tags(o) 0]
      set ned($key,disp-outlinecolor) [lindex $tags(o) 1]
      set ned($key,disp-linethickness) [lindex $tags(o) 2]
   }
}


# parse_submod_dispstr --
#
# update a 'submod' ned element with values from its display string
#
proc parse_submod_dispstr {key} {
   global ned

   split_dispstr $ned($key,displaystring) tags

   # GNED currently only handles only few values from a dispstr...
   if [info exist tags(p)] {
      set ned($key,disp-xpos) [lindex $tags(p) 0]
      set ned($key,disp-ypos) [lindex $tags(p) 1]
   }
   if [info exist tags(b)] {
      set ned($key,disp-xsize) [lindex $tags(b) 0]
      set ned($key,disp-ysize) [lindex $tags(b) 1]
   }
   if [info exist tags(i)] {
      set ned($key,disp-icon) [lindex $tags(i) 0]
   }
   if [info exist tags(o)] {
      set ned($key,disp-fillcolor) [lindex $tags(o) 0]
      set ned($key,disp-outlinecolor) [lindex $tags(o) 1]
      set ned($key,disp-linethickness) [lindex $tags(o) 2]
   }
}

# parse_conn_dispstr --
#
# update a 'conn' ned element with values from its display string
#
proc parse_conn_dispstr {key} {
   global ned

   split_dispstr $ned($key,displaystring) tags

   # GNED currently only handles only few values from a dispstr...
   if [info exist tags(m)] {
      set ned($key,disp-drawmode) [lindex $tags(m) 0]
      set ned($key,disp-src-anchor-x) [lindex $tags(m) 1]
      set ned($key,disp-src-anchor-y) [lindex $tags(m) 2]
      set ned($key,disp-dest-anchor-x) [lindex $tags(m) 3]
      set ned($key,disp-dest-anchor-y) [lindex $tags(m) 4]
   }

   if [info exist tags(o)] {
      set ned($key,disp-fillcolor) [lindex $tags(o) 0]
      set ned($key,disp-linethickness) [lindex $tags(o) 1]
   }
}


# _setlistitem --
#
# private proc.
# Replace an element of a list. If the list is not long enough,
# fill gap with empty items
#
proc _setlistitem {listvar index value} {
   upvar $listvar list

   while {[llength $list]<=$index} {
      lappend list {}
   }
   set list [lreplace $list $index $index $value]
}

# update_module_dispstr --
#
# update display string of a 'module' ned element
#
proc update_module_dispstr {key} {
   global ned

   set order [split_dispstr $ned($key,displaystring) tags]

   if ![info exist tags(p)] {set tags(p) {}}
   _setlistitem tags(p) 0 $ned($key,disp-xpos)
   _setlistitem tags(p) 1 $ned($key,disp-ypos)

   if ![info exist tags(b)] {set tags(b) {}}
   _setlistitem tags(b) 0 $ned($key,disp-xsize)
   _setlistitem tags(b) 1 $ned($key,disp-ysize)

   if ![info exist tags(o)] {set tags(o) {}}
   _setlistitem tags(o) 0 $ned($key,disp-fillcolor)
   _setlistitem tags(o) 1 $ned($key,disp-outlinecolor)
   _setlistitem tags(o) 2 $ned($key,disp-linethickness)

   set ned($key,displaystring) [assemble_dispstr tags $order]
}

# update_submod_dispstr --
#
# update display string of a 'submod' ned element
#
proc update_submod_dispstr {key} {
   global ned

   set order [split_dispstr $ned($key,displaystring) tags]

   if ![info exist tags(p)] {set tags(p) {}}
   _setlistitem tags(p) 0 $ned($key,disp-xpos)
   _setlistitem tags(p) 1 $ned($key,disp-ypos)

   if ![info exist tags(b)] {set tags(b) {}}
   _setlistitem tags(b) 0 $ned($key,disp-xsize)
   _setlistitem tags(b) 1 $ned($key,disp-ysize)

   if ![info exist tags(o)] {set tags(o) {}}
   _setlistitem tags(o) 0 $ned($key,disp-fillcolor)
   _setlistitem tags(o) 1 $ned($key,disp-outlinecolor)
   _setlistitem tags(o) 2 $ned($key,disp-linethickness)

   if ![info exist tags(i)] {set tags(i) {}}
   _setlistitem tags(i) 0 $ned($key,disp-icon)

   set ned($key,displaystring) [assemble_dispstr tags $order]
}


# update_conn_dispstr --
#
# update display string of a 'conn' ned element
#
proc update_conn_dispstr {key} {
   global ned

   set order [split_dispstr $ned($key,displaystring) tags]

   if ![info exist tags(m)] {set tags(m) {}}
   _setlistitem tags(m) 0 $ned($key,disp-drawmode)
   _setlistitem tags(m) 1 $ned($key,disp-src-anchor-x)
   _setlistitem tags(m) 2 $ned($key,disp-src-anchor-y)
   _setlistitem tags(m) 3 $ned($key,disp-dest-anchor-x)
   _setlistitem tags(m) 4 $ned($key,disp-dest-anchor-y)

   if ![info exist tags(o)] {set tags(o) {}}
   _setlistitem tags(o) 0 $ned($key,disp-fillcolor)
   _setlistitem tags(o) 1 $ned($key,disp-linethickness)

   set ned($key,displaystring) [assemble_dispstr tags $order]
}


# parse_displaystrings --
#
# parse display strings in a whole NED tree
#
proc parse_displaystrings {key} {
   debug "parsing display strings..."
   parse_displaystrings_rec $key
}

proc parse_displaystrings_rec {key} {
   global ned

   # parse displaystrings
   set type $ned($key,type)
   if {$type=="module"} {
       parse_module_dispstr $key
   } elseif {$type=="submod"} {
       parse_submod_dispstr $key
   } elseif {$type=="conn"} {
       parse_conn_dispstr $key
   }

   # process children
   foreach childkey $ned($key,childrenkeys) {
       parse_displaystrings_rec $childkey
   }
}


# update_displaystrings --
#
# update display strings in a whole NED tree
#
proc update_displaystrings {key} {
   debug "updating display strings..."
   update_displaystrings_rec $key
}

proc update_displaystrings_rec {key} {
   global ned

   # update displaystrings
   set type $ned($key,type)
   if {$type=="module"} {
       update_module_dispstr $key
   } elseif {$type=="submod"} {
       update_submod_dispstr $key
   } elseif {$type=="conn"} {
       update_conn_dispstr $key
   }

   # process children
   foreach childkey $ned($key,childrenkeys) {
       update_displaystrings_rec $childkey
   }
}

