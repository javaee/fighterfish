#!/bin/bash +x
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

set +e
if [ $# -eq 0 ] 
then
 echo "Usage: $0 <glassfish.home>"
 exit 1
fi
GF_HOME=$1
shift 1
# Some constants
# Where is the workspace?
WS=~/WS/ff/trunk/test/it
OUT=${WS}/t.log
# Total no. of tests in the test suite
EXPECTED_RESULT=`cat $WS/ExpectedTestResult.txt`

# Arguments: ResultVariableName
function equinox() {
 echo "Running tests on Equinox"
 local var_name=$1
 shift 1
 cat ${OUT} >> ${OUT}.old
 rm ${OUT}
 redirect.sh ${OUT} ~/software/apache-maven-2.2.1/bin/mvn -o -f ${WS}/pom.xml clean test -P-Felix -PEquinox -Dglassfish.home=${GF_HOME} $*
 rm -rf ${WS}/surefire-reports-Equinox || true
 mv ${WS}/target/surefire-reports ${WS}/surefire-reports-Equinox
 #Make sure we have run all the tests.
 testReport ${OUT} ${var_name}
 return $?
}

# Arguments: ResultVariableName
function felix() {
 echo "Running tests on Felix"
 local var_name=$1
 shift 1
 cat ${OUT} >> ${OUT}.old
 rm ${OUT}
 redirect.sh ${OUT}  ~/software/apache-maven-2.2.1/bin/mvn -o -f ${WS}/pom.xml clean test -Dglassfish.home=${GF_HOME} $*
 rm -rf ${WS}/surefire-reports-Felix || true
 mv ${WS}/target/surefire-reports ${WS}/surefire-reports-Felix
 # Make sure we have run all the tests. Look at the last line as multiple lines match the pattern
 testReport ${OUT} ${var_name}
 return $?
}

# Arguments: ${OUT} ResultVariableName
function testReport() {
 local  __resultvar=$2
 local report=`grep "Tests run" $1`
 local return_val=0
 ACTUAL_RESULT=`grep "Tests run" $1 | tail -1`
 if [ "$ACTUAL_RESULT" != "$EXPECTED_RESULT" ]
 then
  mismatch="FAILED: Expected and actual results differ: Actual=$ACTUAL_RESULT vs. Expected=$EXPECTED_RESULT"
  report="$report 
$mismatch"
  return_val=1
 fi
 eval $__resultvar="'$report'"
 return $return_val
}

touch ${OUT}
if [ -w ${OUT} ]
then
 felix FelixResult $*
 Felix=$?
 echo Skipping equinox for now due to GLASSFISH-18975
# equinox EquinoxResult $*
 Equinox=$?
 echo "*************SUMARRY***************"
 echo ""
 echo "Felix Test Result:"
 echo "$FelixResult"
 echo ""
 echo "Equinox Test Result:"
 echo "$EquinoxResult"
 echo ""
 exit `expr $Felix + $Equinox`
else
 echo "can't write t.log."
 exit 1
fi
