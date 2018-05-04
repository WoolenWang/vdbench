#!/bin/ksh
#

#
#
# Copyright 2010 Sun Microsystems, Inc. All rights reserved.
#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# The contents of this file are subject to the terms of the Common
# Development and Distribution License("CDDL") (the "License").
# You may not use this file except in compliance with the License.
#
# You can obtain a copy of the License at http://www.sun.com/cddl/cddl.html
# or ../vdbench/license.txt. See the License for the
# specific language governing permissions and limitations under the License.
#
# When distributing the software, include this License Header Notice
# in each file and include the License file at ../vdbench/licensev1.0.txt.
#
# If applicable, add the following below the License Header, with the
# fields enclosed by brackets [] replaced by your own identifying information:
# "Portions Copyrighted [year] [name of copyright owner]"
#

#
# Author: Henk Vandenbergh.
#

# Pick up the proper parent directory from this current script:
dir=`dirname $0`
cd $dir
cd ..
LIB=`pwd`
echo LIB: $LIB

INCS="-I$LIB/Jni -I/usr/java/include/ -I/usr/java/include/solaris "


CC="cc"
CC="/usr/dist/share/sunstudio_sparc,v12.0/SUNWspro/bin/cc"


cd /tmp
rm *.o

echo Starting compiles

$CC                        -c -g -xCC $INCS $LIB/Jni/solvtoc.c  -DSOLARIS

$CC -D_FILE_OFFSET_BITS=64 -c -g -xCC $INCS $LIB/Jni/vdb_dv.c   -DSOLARIS

$CC -D_FILE_OFFSET_BITS=64 -c -g -xCC $INCS $LIB/Jni/vdb.c      -DSOLARIS

$CC -D_FILE_OFFSET_BITS=64 -c -g -xCC $INCS $LIB/Jni/kstat.c    -DSOLARIS

$CC -D_FILE_OFFSET_BITS=64 -c -g -xCC $INCS $LIB/Jni/vdbjni.c   -DSOLARIS -DKSTAT

$CC -D_FILE_OFFSET_BITS=64 -c -g -xCC $INCS $LIB/Jni/kstatcpu.c -DSOLARIS

$CC -D_FILE_OFFSET_BITS=64 -c -g -xCC $INCS $LIB/Jni/vdbsol.c   -DSOLARIS

$CC -D_FILE_OFFSET_BITS=64 -c -g -xCC $INCS $LIB/Jni/nfs_kstat.c  -DSOLARIS


echo Starting link libvdbench.so
$CC  -o  $LIB/solaris/libvdbench.so -mt -G -ladm  -lkstat vdb.o solvtoc.o \
vdb_dv.o vdbjni.o vdbsol.o kstat.o kstatcpu.o nfs_kstat.o -ldl


chmod 777 $LIB/solaris/*


echo Completed. Check for messages


