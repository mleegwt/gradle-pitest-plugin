/* Copyright (c) 2017 Marcin Zajączkowski
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package info.solidsoft.gradle.pitest

import spock.lang.Ignore
import spock.lang.Issue

class PitestPluginClasspathFilteringSpec extends BasicProjectBuilderSpec {

    @Issue('https://github.com/szpak/gradle-pitest-plugin/issues/52')
    def "should filter dynamic library '#libFileName' by default"() {
        given:
            File libFile = addFileWithFileNameAsDependencyAndReturnAsFile(libFileName)
        and:
            PitestTask task = getJustOnePitestTaskOrFail()
        expect:
            !forceClasspathResolutionAndReturnIt(task).contains(libFile.path)
        where:
            libFileName << ['lib.so', 'win.dll', 'dyn.dylib']   //TODO: Add test with more than one element
    }

    def "should filter .pom file by default"() {
        given:
            File pomFile = addFileWithFileNameAsDependencyAndReturnAsFile('foo.pom')
        and:
            PitestTask task = getJustOnePitestTaskOrFail()
        expect:
            !forceClasspathResolutionAndReturnIt(task).contains(pomFile.path)
    }

    def "should not filter regular dependency '#depFileName' by default"() {
        given:
            File depFile = addFileWithFileNameAsDependencyAndReturnAsFile(depFileName)
        and:
            PitestTask task = getJustOnePitestTaskOrFail()
        expect:
            forceClasspathResolutionAndReturnIt(task).contains(depFile.path)
        where:
            depFileName << ['foo.jar', 'foo.zip']
    }

    def "should not filter source set directory by default"() {
        given:
            File testClassesDir = new File(new File(new File(new File(tmpProjectDir.root, 'build'), 'classes'), 'java'), 'test')
        and:
            PitestTask task = getJustOnePitestTaskOrFail()
        expect:
            forceClasspathResolutionAndReturnIt(task).contains(testClassesDir.path)
    }

    def "should filter excluded dependencies remaining regular ones"() {
        given:
            File depFile = addFileWithFileNameAsDependencyAndReturnAsFile('foo.jar')
        and:
            File libDepFile = addFileWithFileNameAsDependencyAndReturnAsFile('bar.so')
        and:
            PitestTask task = getJustOnePitestTaskOrFail()
        expect:
            forceClasspathResolutionAndReturnIt(task).contains(depFile.path)
            !forceClasspathResolutionAndReturnIt(task).contains(libDepFile.path)
    }

    @Issue('https://github.com/szpak/gradle-pitest-plugin/issues/53')
    @Ignore("Not supported in with ListProperty - https://github.com/gradle/gradle/issues/10475")
    def "should filter user defined extensions"() {
        given:
            File depFile = addFileWithFileNameAsDependencyAndReturnAsFile('file.extra')
        and:
            project.pitest.fileExtensionsToFilter += ['extra']
        and:
            PitestTask task = getJustOnePitestTaskOrFail()
        expect:
            !forceClasspathResolutionAndReturnIt(task).contains(depFile.path)
    }

    @Issue('https://github.com/szpak/gradle-pitest-plugin/issues/53')
    def "should filter user defined extensions (property syntax))"() {
        given:
            File depFile = addFileWithFileNameAsDependencyAndReturnAsFile('file.extra')
        and:
            project.pitest.addFileExtensionsToFilter(['extra'])
        and:
            PitestTask task = getJustOnePitestTaskOrFail()
        expect:
            !forceClasspathResolutionAndReturnIt(task).contains(depFile.path)
    }

    @Issue('https://github.com/szpak/gradle-pitest-plugin/issues/53')
    def "should allow to override extensions filtered by default"() {
        given:
            File depFile = addFileWithFileNameAsDependencyAndReturnAsFile('needed.so')
        and:
            project.pitest.fileExtensionsToFilter = ['extra']
        and:
            PitestTask task = getJustOnePitestTaskOrFail()
        expect:
            forceClasspathResolutionAndReturnIt(task).contains(depFile.path)
    }

    @Issue('https://github.com/szpak/gradle-pitest-plugin/issues/53')
    @Ignore("Not supported in with ListProperty - https://github.com/gradle/gradle/issues/10475")
    def "should allow to provide extra extensions in addition to default ones"() {
        given:
            File libDepFile = addFileWithFileNameAsDependencyAndReturnAsFile('default.so')
            File extraDepFile = addFileWithFileNameAsDependencyAndReturnAsFile('file.extra')
        and:
            project.pitest.fileExtensionsToFilter += ['extra']
        and:
            PitestTask task = getJustOnePitestTaskOrFail()
        expect:
            String resolvedPitClasspath = forceClasspathResolutionAndReturnIt(task)
            !resolvedPitClasspath.contains(libDepFile.path)
            !resolvedPitClasspath.contains(extraDepFile.path)
    }

    @Issue('https://github.com/szpak/gradle-pitest-plugin/issues/53')
    def "should allow to provide extra extensions in addition to default ones (property syntax)"() {
        given:
            File libDepFile = addFileWithFileNameAsDependencyAndReturnAsFile('default.so')
            File extraDepFile = addFileWithFileNameAsDependencyAndReturnAsFile('file.extra')
        and:
            project.pitest.addFileExtensionsToFilter(['extra'])
        and:
            PitestTask task = getJustOnePitestTaskOrFail()
        expect:
            String resolvedPitClasspath = forceClasspathResolutionAndReturnIt(task)
            !resolvedPitClasspath.contains(libDepFile.path)
            !resolvedPitClasspath.contains(extraDepFile.path)
    }

    @Issue('https://github.com/szpak/gradle-pitest-plugin/issues/53')
    def "should not fail on fileExtensionsToFilter set to null"() {
        given:
            project.pitest.fileExtensionsToFilter = null
        and:
            PitestTask task = getJustOnePitestTaskOrFail()
        when:
            String resolvedPitClasspath = forceClasspathResolutionAndReturnIt(task)
        then:
            noExceptionThrown()
        and:
            resolvedPitClasspath.contains('main')
    }

    def "should filter dependencies also from 'api' configuration in java-library"() {
        given:
            project.apply(plugin: "java-library")   //to add 'api' configuration
        and:
            File libFile = addFileWithFileNameAsDependencyAndReturnAsFile('lib.so', 'api')
        and:
            PitestTask task = getJustOnePitestTaskOrFail()
        expect:
            !forceClasspathResolutionAndReturnIt(task).contains(libFile.path)
    }

    private String forceClasspathResolutionAndReturnIt(PitestTask task) {
        return task.createTaskArgumentMap()['classPath']
    }

    private File addFileWithFileNameAsDependencyAndReturnAsFile(String depFileName, String configurationName = 'implementation') {
        File depFile = new File(tmpProjectDir.root, depFileName)
        project.dependencies.add(configurationName, project.files(depFile))
        return depFile
    }
}
