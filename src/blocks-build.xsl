<?xml version="1.0"?>
<!--
  Copyright 1999-2004 The Apache Software Foundation

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:output method="xml" indent="yes"/>

  <xsl:template match="/">
    <project default="compile" basedir="." name="blocks">
      <description>Autogenerated Ant build file that builds blocks.</description>

      <path id="classpath">
        <fileset dir="${{lib.core}}">
          <include name="*.jar"/>
        </fileset>
        <fileset dir="${{lib.endorsed}}">
          <include name="*.jar"/>
        </fileset>
        <!-- Currently, we have no JVM dependent libraries
          <fileset dir="${{lib.core}}/jvm${{target.vm}}">
             <include name="*.jar"/>
          </fileset>
        -->
        <fileset dir="${{lib.optional}}">
          <include name="*.jar"/>
        </fileset>
        <fileset dir="${{build.blocks}}">
          <include name="*.jar"/>
        </fileset>
        <path location="${{build.mocks}}"/>
        <path location="${{build.dest}}"/>
        <pathelement path="${{build.webapp.webinf}}/classes/"/>

        <xsl:for-each select="module/project[starts-with(@name, 'cocoon-block-')]">
          <xsl:variable name="block-name" select="substring-after(@name,'cocoon-block-')"/>
          <fileset dir="${{blocks}}/{$block-name}">
            <include name="lib/*.jar"/>
          </fileset>
          <pathelement location="${{build.blocks}}/{$block-name}/dest"/>
          <pathelement location="${{build.blocks}}/{$block-name}/samples"/>
          <pathelement location="${{build.blocks}}/{$block-name}/mocks"/>
        </xsl:for-each>
      </path>

      <path id="test.classpath">
        <fileset dir="${{tools.lib}}">
          <include name="*.jar"/>
        </fileset>
      </path>

      <!-- Files, which should no compiled or otherwise processed -->
      <patternset id="unprocessed.sources">
        <exclude name="**/*.java"/>
        <exclude name="**/*.xconf"/>
        <exclude name="**/*.xroles"/>
        <exclude name="**/*.xmap"/>
        <exclude name="**/*.xpipe"/>
        <exclude name="**/*.xlog"/>
        <exclude name="**/*.xweb"/>
        <exclude name="**/package.html"/>
      </patternset>

      <target name="init">
        <xsl:for-each select="module/project[starts-with(@name, 'cocoon-block-')]">
          <xsl:variable name="block-name" select="substring-after(@name,'cocoon-block-')"/>
          <condition property="unless.exclude.block.{$block-name}">
            <istrue value="${{exclude.block.{$block-name}}}"/>
          </condition>
        </xsl:for-each>
      </target>

      <xsl:apply-templates select="module"/>
    </project>
  </xsl:template>

  <xsl:template match="module">
    <xsl:variable name="cocoon-blocks" select="project[starts-with(@name, 'cocoon-block-')]"/>

    <target name="compile">
      <xsl:attribute name="depends">
        <xsl:text>init</xsl:text>
        <xsl:for-each select="$cocoon-blocks">
          <xsl:text>,</xsl:text>
          <xsl:value-of select="concat(@name, '-compile')"/>
        </xsl:for-each>
      </xsl:attribute>
    </target>

    <target name="patch">
      <xsl:attribute name="depends">
        <xsl:text>init</xsl:text>
        <xsl:for-each select="$cocoon-blocks">
          <xsl:text>,</xsl:text>
          <xsl:value-of select="concat(@name, '-patch')"/>
        </xsl:for-each>
      </xsl:attribute>
    </target>
                                                                                                                                                                               
    <target name="roles">
      <xsl:attribute name="depends">
        <xsl:text>init</xsl:text>
        <xsl:for-each select="$cocoon-blocks">
          <xsl:text>,</xsl:text>
          <xsl:value-of select="concat(@name, '-roles')"/>
        </xsl:for-each>
      </xsl:attribute>
    </target>

    <target name="patch-samples">
      <xsl:attribute name="depends">
        <xsl:text>init</xsl:text>
        <xsl:for-each select="$cocoon-blocks">
          <xsl:text>,</xsl:text>
          <xsl:value-of select="concat(@name, '-patch-samples')"/>
        </xsl:for-each>
      </xsl:attribute>
    </target>

    <target name="samples">
      <xsl:attribute name="depends">
        <xsl:text>init,patch-samples</xsl:text>
        <xsl:for-each select="$cocoon-blocks">
          <xsl:text>,</xsl:text>
          <xsl:value-of select="concat(@name, '-samples')"/>
        </xsl:for-each>
      </xsl:attribute>
    </target>

    <target name="lib">
      <xsl:attribute name="depends">
        <xsl:text>init</xsl:text>
        <xsl:for-each select="$cocoon-blocks">
          <xsl:text>,</xsl:text>
          <xsl:value-of select="concat(@name, '-lib')"/>
        </xsl:for-each>
      </xsl:attribute>
    </target>

    <target name="tests">
      <xsl:attribute name="depends">
        <xsl:text>init</xsl:text>
        <xsl:for-each select="$cocoon-blocks">
          <xsl:text>,</xsl:text>
          <xsl:value-of select="concat(@name, '-tests')"/>
        </xsl:for-each>
      </xsl:attribute>
    </target>

    <target name="prepare-anteater-tests">
      <xsl:attribute name="depends">
        <xsl:text>init</xsl:text>
        <xsl:for-each select="$cocoon-blocks">
          <xsl:text>,</xsl:text>
          <xsl:value-of select="concat(@name, '-prepare-anteater-tests')"/>
        </xsl:for-each>
      </xsl:attribute>
    </target>

    <!-- Check if javadocs have to be generated -->
    <target name="javadocs-check">
      <mkdir dir="${{build.javadocs}}"/>
      <condition property="javadocs.notrequired" value="true">
        <or>
          <uptodate targetfile="${{build.javadocs}}/packages.html">
            <srcfiles dir="${{java}}" includes="**/*.java,**/package.html"/>
            <srcfiles dir="${{deprecated.src}}" includes="**/*.java,**/package.html"/>
            <xsl:for-each select="$cocoon-blocks">
              <srcfiles dir="${{blocks}}/{substring-after(@name,'cocoon-block-')}/java" includes="**/*.java,**/package.html"/>
            </xsl:for-each>
          </uptodate>
          <istrue value="${{unless.exclude.javadocs}}"/>
        </or>
      </condition>
    </target>

    <!-- Creates Javadocs -->
    <target name="javadocs" unless="javadocs.notrequired">
      <xsl:attribute name="depends">
        <xsl:text>init, javadocs-check</xsl:text>
        <xsl:for-each select="$cocoon-blocks">
          <xsl:text>,</xsl:text>
          <xsl:value-of select="concat(substring-after(@name, 'cocoon-block-'), '-prepare')"/>
        </xsl:for-each>
      </xsl:attribute>

      <condition property="javadoc.additionalparam" value="-breakiterator -tag todo:all:Todo:">
        <equals arg1="1.4" arg2="${{ant.java.version}}"/>
      </condition>
      <condition property="javadoc.additionalparam" value="">
        <not>
          <equals arg1="1.4" arg2="${{ant.java.version}}"/>
        </not>
      </condition>

      <javadoc destdir="${{build.javadocs}}"
               author="true"
               version="true"
               use="true"
               noindex="false"
               splitindex="true"
               windowtitle="${{Name}} API ${{version}} [${{TODAY}}]"
               doctitle="${{Name}} API ${{version}}"
               bottom="Copyright &#169; ${{year}} The Apache Software Foundation. All Rights Reserved."
               stylesheetfile="${{resources.javadoc}}/javadoc.css"
               useexternalfile="yes"
               additionalparam="${{javadoc.additionalparam}}"
               maxmemory="128m">
               
        <link offline="true" href="http://avalon.apache.org/framework/api" packagelistloc="${{resources.javadoc}}/avalon-framework"/>
        <link offline="true" href="http://avalon.apache.org/excalibur/api" packagelistloc="${{resources.javadoc}}/avalon-excalibur"/>
        <link offline="true" href="http://xml.apache.org/xerces2-j/javadocs/api" packagelistloc="${{resources.javadoc}}/xerces"/>
        <link offline="true" href="http://xml.apache.org/xalan-j/apidocs" packagelistloc="${{resources.javadoc}}/xalan"/>
        <link offline="true" href="http://java.sun.com/j2se/1.4.2/docs/api" packagelistloc="${{resources.javadoc}}/j2se"/>
        <link offline="true" href="http://java.sun.com/j2ee/sdk_1.3/techdocs/api" packagelistloc="${{resources.javadoc}}/j2ee"/>

        <tag name="avalon.component"   scope="types"   description="Avalon component" />
        <tag name="avalon.service"     scope="types"   description="Implements service:" />
        <!-- FIXME: javadoc or ant seems to not understand these
        <tag name="x-avalon.info"      scope="types"   description="Shorthand:" />
        <tag name="x-avalon.lifestyle" scope="types"   description="Lifestyle:" />
        -->
        <tag name="avalon.context"     scope="methods" description="Requires entry:" />
        <tag name="avalon.dependency"  scope="methods" description="Requires component:" />
      
        <packageset dir="${{java}}">
          <include name="**"/>
        </packageset>
        <packageset dir="${{deprecated.src}}">
          <include name="**"/>
        </packageset>
        <xsl:for-each select="$cocoon-blocks">
          <packageset dir="${{blocks}}/{substring-after(@name,'cocoon-block-')}/java">
            <include name="**"/>
          </packageset>
        </xsl:for-each>
        <classpath>
          <fileset dir="${{tools.lib}}">
            <include name="*.jar"/>
          </fileset>
          <path refid="classpath" />
        </classpath>
      </javadoc>
    </target>

    <xsl:apply-templates select="$cocoon-blocks"/>
  </xsl:template>

  <xsl:template match="project">
    <xsl:variable name="block-name" select="substring-after(@name,'cocoon-block-')"/>
    <xsl:variable name="cocoon-block-dependencies" select="depend[starts-with(@project,'cocoon-block-')]"/>

    <target name="{@name}-excluded" if="exclude.block.{$block-name}">
      <echo message="NOTICE: Block '{$block-name}' is excluded from the build."/>
    </target>

    <target name="{@name}" unless="unless.exclude.block.{$block-name}"/>

    <target name="{@name}-compile" unless="unless.exclude.block.{$block-name}">
      <xsl:if test="depend">
        <xsl:attribute name="depends">
          <xsl:value-of select="@name"/>,<xsl:value-of select="@name"/>-excluded<xsl:text/>
          <xsl:for-each select="$cocoon-block-dependencies">
            <xsl:text>,</xsl:text>
            <xsl:value-of select="concat(@project, '-compile')"/>
          </xsl:for-each>
        </xsl:attribute>
      </xsl:if>

      <!-- Test if this block has special build -->
      <available property="{$block-name}.has.build" file="${{blocks}}/{$block-name}/build.xml"/>

      <!-- Test if this block has mocks -->
      <available property="{$block-name}.has.mocks" type="dir" file="${{blocks}}/{$block-name}/mocks/"/>

      <xsl:if test="@status='unstable'">
        <echo message="==================== WARNING ======================="/>
        <echo message=" Block '{$block-name}' should be considered unstable."/>
        <echo message="----------------------------------------------------"/>
        <echo message="         This means that its API, schemas "/>
        <echo message="  and other contracts might change without notice."/>
        <echo message="===================================================="/>
      </xsl:if>

      <antcall target="{$block-name}-compile"/>
    </target>

    <target name="{@name}-patch" unless="unless.exclude.block.{$block-name}">
      <xsl:attribute name="depends"><xsl:value-of select="$block-name"/>-prepare<xsl:if test="depend">,<xsl:value-of select="@name"/><xsl:for-each select="depend[contains(@project,'cocoon-block-')]"><xsl:text>,</xsl:text><xsl:value-of select="@project"/>-patch</xsl:for-each></xsl:if></xsl:attribute>
                                                                                                                                                                               
      <xpatch file="${{build.webapp}}/sitemap.xmap" srcdir="${{blocks}}">
        <include name="{$block-name}/conf/*.xmap"/>
      </xpatch>
      <xpatch file="${{build.webapp}}/WEB-INF/cocoon.xconf" srcdir="${{blocks}}" addcomments="true">
        <include name="{$block-name}/conf/*.xconf"/>
      </xpatch>
      <xpatch file="${{build.webapp}}/WEB-INF/logkit.xconf" srcdir="${{blocks}}">
        <include name="{$block-name}/conf/*.xlog"/>
      </xpatch>
      <xpatch file="${{build.webapp}}/WEB-INF/web.xml" srcdir="${{blocks}}">
        <include name="{$block-name}/conf/*.xweb"/>
      </xpatch>

    </target>
                                                                                                                                                                               
    <target name="{@name}-roles" unless="unless.exclude.block.{$block-name}">
      <xsl:if test="depend">
        <xsl:attribute name="depends"><xsl:value-of select="@name"/><xsl:for-each select="depend[contains(@project,'cocoon-block-')]"><xsl:text>,</xsl:text><xsl:value-of select="@project"/>-roles</xsl:for-each></xsl:attribute>
      </xsl:if>
                                                                                                                                                                               
      <xpatch file="${{build.dest}}/org/apache/cocoon/cocoon.roles" srcdir="${{blocks}}">
        <include name="{$block-name}/conf/*.xroles"/>
      </xpatch>
    </target>

    <target name="{@name}-patch-samples" unless="unless.exclude.block.{$block-name}">
      <xpatch file="${{build.webapp}}/samples/block-samples.xml" srcdir="${{blocks}}">
        <include name="{$block-name}/conf/*.xsamples"/>
      </xpatch>
      <xpatch file="${{build.webapp}}/samples/sitemap.xmap" srcdir="${{blocks}}">
        <include name="{$block-name}/conf/*.samplesxpipe"/>
      </xpatch>
      <xpatch file="${{build.webapp}}/WEB-INF/cocoon.xconf" srcdir="${{blocks}}">
        <include name="{$block-name}/conf/*.samplesxconf"/>
      </xpatch>
    </target>

    <target name="{@name}-samples" unless="unless.exclude.block.{$block-name}">
      <xsl:if test="depend">
        <xsl:attribute name="depends">
          <xsl:value-of select="@name"/>
          <xsl:for-each select="$cocoon-block-dependencies">
            <xsl:text>,</xsl:text>
            <xsl:value-of select="concat(@project, '-samples')"/>
          </xsl:for-each>
        </xsl:attribute>
      </xsl:if>

      <!-- Test if this block has samples -->
      <if>
        <available file="${{blocks}}/{$block-name}/samples/sitemap.xmap"/>
        <then>
          <copy filtering="on" todir="${{build.webapp}}/samples/{$block-name}">
            <fileset dir="${{blocks}}/{$block-name}/samples"/>
          </copy>

          <!-- copy sample classes -->
          <copy todir="${{build.webapp.classes}}" filtering="off">
            <fileset dir="${{build.blocks}}/{$block-name}/samples"/>
          </copy>
        </then>
      </if>
    </target>

    <target name="{@name}-lib" unless="unless.exclude.block.{$block-name}">
      <xsl:if test="depend">
        <xsl:attribute name="depends">
          <xsl:value-of select="@name"/>
          <xsl:for-each select="$cocoon-block-dependencies">
            <xsl:text>,</xsl:text>
            <xsl:value-of select="concat(@project, '-lib')"/>
          </xsl:for-each>
        </xsl:attribute>
      </xsl:if>

      <!-- Test if this block has libraries -->
      <if>
        <available type="dir" file="${{blocks}}/{$block-name}/lib/"/>
        <then>
          <copy filtering="off" todir="${{build.webapp.lib}}">
            <fileset dir="${{blocks}}/{$block-name}/lib">
              <include name="*.jar"/>
              <exclude name="servlet*.jar"/>
            </fileset>
          </copy>
        </then>
      </if>

      <!-- Test if this block has global WEB-INF files -->
      <if>
        <available type="dir" file="${{blocks}}/{$block-name}/WEB-INF/"/>
        <then>
          <copy filtering="on" todir="${{build.webapp.webinf}}">
            <fileset dir="${{blocks}}/{$block-name}/WEB-INF/">
              <include name="**"/>
            </fileset>
          </copy>
        </then>
      </if>

    </target>

    <target name="{$block-name}-prepare" unless="unless.exclude.block.{$block-name}">
      <xsl:if test="depend">
        <xsl:attribute name="depends">
          <xsl:value-of select="@name"/>
          <xsl:for-each select="$cocoon-block-dependencies">
            <xsl:text>,</xsl:text>
            <xsl:value-of select="concat(substring-after(@project,'cocoon-block-'), '-prepare')"/>
          </xsl:for-each>
        </xsl:attribute>
      </xsl:if>

      <!-- Test if this block has mocks -->
      <available property="{$block-name}.has.mocks" type="dir" file="${{blocks}}/{$block-name}/mocks/"/>

      <mkdir dir="${{build.blocks}}/{$block-name}/dest"/>
    </target>

    <target name="{$block-name}-compile" depends="{$block-name}-build,{$block-name}-prepare,{$block-name}-mocks">

      <!-- This is a little bit tricky:
           As the javac task checks, if a src directory is available and
           stops if its not available, we use the following property
           to either point to a jdk dependent directory or - if not
           available - to the usual java source directory.
           If someone knows a better solution...
      -->
      <!-- Currently, we have no JVM dependent sources
      <condition property="dependend.vm" value="${{target.vm}}">
        <available file="${{blocks}}/{$block-name}/java${{target.vm}}"/>
      </condition>
      <condition property="dependend.vm" value="">
        <not>
          <available file="${{blocks}}/{$block-name}/java${{target.vm}}"/>
        </not>
      </condition>
      -->
      <javac destdir="${{build.blocks}}/{$block-name}/dest"
             debug="${{compiler.debug}}"
             optimize="${{compiler.optimize}}"
             deprecation="${{compiler.deprecation}}"
             target="${{target.vm}}"
             nowarn="${{compiler.nowarn}}"
             compiler="${{compiler}}">
        <src path="${{blocks}}/{$block-name}/java"/>
        <!-- Currently, we have no JVM dependent sources
        <src path="${{blocks}}/{$block-name}/java${{dependend.vm}}"/>
        -->
        <classpath refid="classpath"/>
        <exclude name="**/samples/**/*.java"/>
      </javac>

      <copy filtering="on" todir="${{build.blocks}}/{$block-name}/dest">
        <fileset dir="${{blocks}}/{$block-name}/java">
          <patternset refid="unprocessed.sources"/>
        </fileset>
      </copy>

      <copy filtering="off" todir="${{build.blocks}}/{$block-name}/dest">
        <fileset dir="${{blocks}}/{$block-name}/java">
          <include name="**/Manifest.mf"/>
          <include name="META-INF/**"/>
        </fileset>
      </copy>

      <jar jarfile="${{build.blocks}}/{$block-name}-block.jar" index="true">
        <fileset dir="${{build.blocks}}/{$block-name}/dest">
          <include name="org/**"/>
          <include name="META-INF/**"/>
        </fileset>
      </jar>

      <!-- exclude sample classes from the block package -->
      <mkdir dir="${{build.blocks}}/{$block-name}/samples"/>
      <javac destdir="${{build.blocks}}/{$block-name}/samples"
             debug="${{compiler.debug}}"
             optimize="${{compiler.optimize}}"
             deprecation="${{compiler.deprecation}}"
             target="${{target.vm}}"
             nowarn="${{compiler.nowarn}}"
             compiler="${{compiler}}">
        <src path="${{blocks}}/{$block-name}/java"/>
        <!-- Currently, we have no JVM dependent sources
        <src path="${{blocks}}/{$block-name}/java${{dependend.vm}}"/>
        -->
        <classpath refid="classpath"/>
        <include name="**/samples/**/*.java"/>
      </javac>
    </target>

    <target name="{$block-name}-build" if="{$block-name}.has.build">
      <ant inheritAll="true"
           inheritRefs="false"
           target="main"
           antfile="${{blocks}}/{$block-name}/build.xml">
        <property name="block.dir" value="${{blocks}}/{$block-name}"/>
      </ant>
    </target>

    <target name="{$block-name}-mocks" depends="{$block-name}-prepare" if="{$block-name}.has.mocks">
      <mkdir dir="${{build.blocks}}/{$block-name}/mocks"/>
      <javac srcdir="${{blocks}}/{$block-name}/mocks"
             destdir="${{build.blocks}}/{$block-name}/mocks"
             debug="${{compiler.debug}}"
             optimize="${{compiler.optimize}}"
             deprecation="${{compiler.deprecation}}"
             target="${{target.vm}}"
             nowarn="${{compiler.nowarn}}"
             compiler="${{compiler}}">
        <classpath refid="classpath"/>
      </javac>
    </target>

    <target name="{@name}-tests" unless="unless.exclude.block.{$block-name}">
      <xsl:if test="depend">
        <xsl:attribute name="depends">
          <xsl:value-of select="@name"/>
          <xsl:for-each select="$cocoon-block-dependencies">
            <xsl:text>,</xsl:text>
            <xsl:value-of select="concat(@project, '-compile')"/>
          </xsl:for-each>
        </xsl:attribute>
      </xsl:if>

      <!-- Test if this block has tests -->
      <available property="{$block-name}.has.tests" file="${{blocks}}/{$block-name}/test"/>

      <antcall target="{$block-name}-tests"/>
    </target>

    <target name="{$block-name}-tests" depends="{$block-name}-compile" if="{$block-name}.has.tests">
      <mkdir dir="${{build.blocks}}/{$block-name}/test"/>

      <copy todir="${{build.blocks}}/{$block-name}/test" filtering="on">
        <fileset dir="${{blocks}}/{$block-name}/test" excludes="**/*.java"/>
      </copy>

      <javac destdir="${{build.blocks}}/{$block-name}/test"
             debug="${{compiler.debug}}"
             optimize="${{compiler.optimize}}"
             deprecation="${{compiler.deprecation}}"
             target="${{target.vm}}"
             nowarn="${{compiler.nowarn}}"
             compiler="${{compiler}}">
        <src path="${{blocks}}/{$block-name}/test"/>
        <classpath>
          <path refid="classpath"/>
          <path refid="test.classpath"/>
          <pathelement location="${{build.test}}"/>
        </classpath>
      </javac>

      <junit printsummary="yes" haltonfailure="yes" fork="yes">
        <jvmarg value="-Djava.endorsed.dirs=lib/endorsed"/>
        <classpath>
          <path refid="classpath"/>
          <path refid="test.classpath"/>
          <pathelement location="${{build.test}}"/>
          <pathelement location="${{build.blocks}}/{$block-name}/test"/>
        </classpath>
        <formatter type="plain" usefile="no"/>
        <batchtest>
          <fileset dir="${{build.blocks}}/{$block-name}/test">
            <include name="**/*TestCase.class"/>
            <include name="**/*Test.class"/>
            <exclude name="**/AllTest.class"/>
            <exclude name="**/*$$*Test.class"/>
            <exclude name="**/Abstract*.class"/>
          </fileset>
        </batchtest>
      </junit>
    </target>
    <target name="{@name}-prepare-anteater-tests" unless="unless.exclude.block.{$block-name}">

      <!-- Test if this block has Anteater tests -->
      <if>
        <available file="${{blocks}}/{$block-name}/test/anteater"/>
        <then>
          <copy todir="${{build.test}}/anteater">
            <fileset dir="${{blocks}}/{$block-name}/test/anteater"/>
            <mapper type="glob" from="*.xml" to="{$block-name}-*.xml"/>
          </copy>
        </then>
      </if>
    </target>
  </xsl:template>
</xsl:stylesheet>
