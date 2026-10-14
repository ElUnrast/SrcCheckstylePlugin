/*
 * Copyright 2026 SRC Security Research & Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.src.gradle.plugins.checkstyle;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.quality.CheckstyleExtension;
import org.gradle.api.plugins.quality.CheckstylePlugin;
import org.gradle.api.plugins.quality.Checkstyle;
import org.gradle.api.tasks.JavaExec;

/**
 * Configures the SRC Checkstyle rules and makes the custom checks available to
 * every Checkstyle task.
 */
public class C_SrcCheckstylePlugin implements Plugin<Project > {
    private static final String k_checkstyleFileProperty = "checkstyleFile";
    private static final String k_checkstyleVersion = "14.1.0";

    @Override
    public void apply( Project project ) {
        project.getPluginManager().apply( CheckstylePlugin.class );
        configureCheckstyleExtension( project );
        registerCheckstyleFileTask( project );
        registerCheckstyleFixFileTask( project );
        configureCheckstyleTasks( project );
    }

    private void configureCheckstyleExtension( Project project ) {
        CheckstyleExtension extension = project.getExtensions().getByType( CheckstyleExtension.class );
        extension.setToolVersion( k_checkstyleVersion );
        extension.setConfig( project.getResources().getText().fromString( getCheckstyleConfiguration() ) );
        extension.setIgnoreFailures( true );
        extension.setMaxWarnings( Integer.MAX_VALUE );
        extension.setMaxErrors( Integer.MAX_VALUE );
    }

    private void registerCheckstyleFileTask( Project project ) {
        project.getTasks().register( "checkstyleFile", Checkstyle.class, task -> {
            task.setGroup( "verification" );
            task.setDescription(
                    "Runs Checkstyle for a single Java file. Usage: -PcheckstyleFile=src/main/java/.../MyClass.java"
            );
            task.setClasspath( project.files() );
            task.setSource( project.files( project.provider( () -> getCheckstyleFile( project ) ) ) );
        } );
    }

    private void registerCheckstyleFixFileTask( Project project ) {
        project.getTasks().register( "checkstyleFixFile", JavaExec.class, task -> {
            task.setGroup( "formatting" );
            task.setDescription(
                    "Applies safe fixes for SRC Checkstyle rules to a single Java file."
                            + " Usage: -PcheckstyleFile=src/main/java/.../MyClass.java"
            );
            task.setClasspath(
                    project.files( getPluginLocation() ).plus( project.getConfigurations().getByName( "checkstyle" ) )
            );
            task.getMainClass().set( "com.src.tools.checkstyle.C_JavaStyleFixer" );
            // args() würde den Provider nur per toString() ("provider(?)") übergeben; erst der ArgumentProvider wertet
            // das Property zur Ausführungszeit aus.
            task.getArgumentProviders().add( () -> List.of( getCheckstyleFile( project ).getAbsolutePath() ) );
        } );
    }

    private void configureCheckstyleTasks( Project project ) {
        File pluginLocation = getPluginLocation();

        project.getTasks().withType( Checkstyle.class ).configureEach( task -> {
            task.setCheckstyleClasspath( task.getCheckstyleClasspath().plus( project.files( pluginLocation ) ) );
            task.getMaxHeapSize().set( "2g" );
            task.getReports().getHtml().getRequired().set( true );
            task.getReports().getXml().getRequired().set( true );
        } );
    }

    private File getCheckstyleFile( Project project ) {
        String checkstyleFileProperty = project.getProviders()
                .gradleProperty( k_checkstyleFileProperty )
                .map( String::trim )
                .getOrNull();

        if( checkstyleFileProperty == null || checkstyleFileProperty.isEmpty() ) {
            throw new GradleException(
                    "Missing -PcheckstyleFile. Usage: g :" + project.getName()
                            + ":checkstyleFile -PcheckstyleFile=src/main/java/.../MyClass.java"
            );
        }

        File fileToCheck = project.file( checkstyleFileProperty );

        if( !fileToCheck.exists() || !fileToCheck.isFile() ) {
            throw new GradleException( "File not found: " + fileToCheck );
        }
        if( !fileToCheck.getName().endsWith( ".java" ) ) {
            throw new GradleException( "Only .java files are supported: " + fileToCheck );
        }

        try {
            String projectDirPath = project.getProjectDir().getCanonicalPath() + File.separator;
            String filePath = fileToCheck.getCanonicalPath();

            if( !filePath.startsWith( projectDirPath ) ) {
                throw new GradleException( "File must be inside module " + project.getName() + ": " + fileToCheck );
            }
        }
        catch( IOException exception ) {
            throw new GradleException( "Cannot resolve Checkstyle file: " + fileToCheck, exception );
        }

        return fileToCheck;
    }

    private String getCheckstyleConfiguration() {
        try( InputStream input = getClass().getResourceAsStream( "/checkstyle.xml" ) ) {
            if( input == null ) {
                throw new GradleException( "Missing Checkstyle configuration resource: checkstyle.xml" );
            }

            return new String( input.readAllBytes(), StandardCharsets.UTF_8 );
        }
        catch( IOException exception ) {
            throw new GradleException( "Cannot read Checkstyle configuration resource", exception );
        }
    }

    private File getPluginLocation() {
        try {
            return new File( getClass().getProtectionDomain().getCodeSource().getLocation().toURI() );
        }
        catch( Exception exception ) {
            throw new GradleException( "Cannot resolve the SRC Checkstyle plugin location", exception );
        }
    }
}
