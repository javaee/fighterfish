#!/bin/sh -ex
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

# Update this everytime you want to promote one or more modules.
# Use space a delim while specifying more than one module dir path and quote the entire variable.
# We hard code the module name to avoid having to update the hudson job
# everytime we want to promote. This also allows us better tracking.
MODULES="\
 module/osgi-javaee-base \
 module/osgi-web-container \
"

GPG_PASSPHRASE=$1 #Take as argument for security reasons

promote_one_module() {
    # MODULE is relative path of module to be promoted wrt fighterfish dir.
    MODULE=$1

    echo promoting $MODULE

    if [ "$MODULE" = "" ]
    then
     echo "Module name missing"
     exit 1
    fi

    GPG_PASSPHRASE=$2
    if [ "$GPG_PASSPHRASE" = "" ]
    then
     echo "GPG Passphrase must be provided or you must change the script to run in interactive mode."
     exit 1
    fi

    # Create a temporary dir to checkout the module(s) that need to be released.
    TS=`date +%Y%m%d_%H%M%S`
    mkdir $WORKSPACE/$TS
    cd $WORKSPACE/$TS
    svn co https://svn.java.net/svn/glassfish~svn/trunk/fighterfish/$MODULE $MODULE

    cd $MODULE

    # We don't use any separate maven local repo, because I don't know how to make release plugin use it in forked processes the special maven repo.
    # So, we use the default one.
    mvn -Dhttps.proxyHost=www-proxy.us.oracle.com -Dhttps.proxyPort=80 -B -DtagBase=https://svn.java.net/svn/glassfish~svn/tags/fighterfish-releases -DtagNameFormat=@{project.groupId}.@{project.artifactId}-@{project.version} -Dgpg.passphrase=$GPG_PASSPHRASE release:prepare 
     
    # We don't use any separate maven local repo, because I don't know how to make release plugin use it in forked processes the special maven repo.
    # So, we use the default one.
    mvn -Dhttps.proxyHost=www-proxy.us.oracle.com -Dhttps.proxyPort=80 -Dgpg.passphrase=$GPG_PASSPHRASE -B release:perform 
}

for MODULE in $MODULES 
do
    promote_one_module $MODULE $GPG_PASSPHRASE
done

