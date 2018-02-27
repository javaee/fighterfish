#!/bin/sh -x
#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
#
# The contents of this file are subject to the terms of either the GNU
# General Public License Version 2 only ("GPL") or the Common Development
# and Distribution License("CDDL") (collectively, the "License").  You
# may not use this file except in compliance with the License.  You can
# obtain a copy of the License at
# https://oss.oracle.com/licenses/CDDL+GPL-1.1
# or LICENSE.txt.  See the License for the specific
# language governing permissions and limitations under the License.
#
# When distributing the software, include this License Header Notice in each
# file and include the License file at LICENSE.txt.
#
# GPL Classpath Exception:
# Oracle designates this particular file as subject to the "Classpath"
# exception as provided by Oracle in the GPL Version 2 section of the License
# file that accompanied this code.
#
# Modifications:
# If applicable, add the following below the License Header, with the fields
# enclosed by brackets [] replaced by your own identifying information:
# "Portions Copyright [year] [name of copyright owner]"
#
# Contributor(s):
# If you wish your version of this file to be governed by only the CDDL or
# only the GPL Version 2, indicate your decision by adding "[Contributor]
# elects to include this software in this distribution under the [CDDL or GPL
# Version 2] license."  If you don't indicate a single choice of license, a
# recipient has the option to distribute your version of this file under
# either the CDDL, the GPL Version 2 or to extend the choice of license to
# its licensees as provided above.  However, if you add GPL Version 2 code
# and therefore, elected the GPL Version 2 license, then the option applies
# only if the new code is made subject to such option by the copyright
# holder.
#

if [ $# -eq 0 ]
then
 echo "Usage: $0 <glassfish.home>"
 exit 1
fi
GF_HOME=$1
shift 1
WS=~/WS/ff/fighterfish-gf3.1.1/test/test.it
mkdir ${GF_HOME}/osgi/felix/conf/
cp ${GF_HOME}/config/osgi.properties ${GF_HOME}/osgi/felix/conf/config.properties
mkdir ${GF_HOME}/osgi/equinox/configuration/
cp ${GF_HOME}/config/osgi.properties ${GF_HOME}/osgi/equinox/configuration/config.ini
OUT=${WS}/t.log
touch ${OUT}
if [ -w ${OUT} ]
then
 rm ${OUT}
 #echo "Running tests on Equinox"
 #redirect.sh ${OUT} `which mvn` -o -f ${WS}/pom.xml clean test -P-Felix -PEquinox -Dglassfish.home=${GF_HOME} $*
 #rm -rf ${WS}/surefire-reports-Equinox || true
 #mv ${WS}/target/surefire-reports ${WS}/surefire-reports-Equinox
 #rm ${GF_HOME}/osgi/equinox/configuration/config.ini
 echo "Running tests on Felix"
 redirect.sh ${OUT} `which mvn` -o -f ${WS}/pom.xml clean test -Dglassfish.home=${GF_HOME} $*
 rm -rf ${WS}/surefire-reports-Felix || true
 mv ${WS}/target/surefire-reports ${WS}/surefire-reports-Felix
 rm ${GF_HOME}/osgi/felix/conf/config.properties
 echo "Summary:" 
 grep "Tests run" ${OUT}
else
 echo "can't write t.log."
 exit 1
fi
