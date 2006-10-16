#
# Build harmony from svn
#
svn co https://svn.apache.org/repos/asf/incubator/harmony/enhanced/trunk Harmony
(cd Harmony; ant populate_source)

(cd Harmony/working_classlib
    svn update
    ant fetch-depends
    ant
)

(cd Harmony/working_vm/build
    cp drlvm.properties.example drlvm.properties
    sh build.sh update
    sh build.sh
)
