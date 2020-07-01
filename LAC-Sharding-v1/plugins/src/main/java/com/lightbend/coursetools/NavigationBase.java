package com.lightbend.coursetools;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public abstract class NavigationBase extends AbstractMojo {
    @Parameter(required = true, property = "exercise")
    protected String exercise;

    protected Path solutionDir = Paths.get("../../solutions/java");
    protected String testPath = "src/test";
    protected String mainPath = "src/main";

    protected Path getSolutionDirectory() {
        try {
            return Files.list(solutionDir)
                .filter(path -> path.getFileName().toString().contains(exercise))
                .findFirst()
                .orElseThrow(() -> new IOException("Unable to locate solution for tag: "+ exercise));
        } catch (IOException ex) {
            System.out.println("UNABLE TO LOCATE SOLUTION IN: "+solutionDir.toAbsolutePath().toString());
            ex.printStackTrace();
            return null;
        }
    }

    protected void purgeFolder(Path folder) {
        try {
            Files.walk(folder)
                    .map(Path::toFile)
                    .sorted((o1, o2) -> -o1.compareTo(o2))
                    .forEach(File::delete);
        } catch (IOException ex) {
            System.out.println("FAILED TO PURGE THE FOLDER: "+folder.toAbsolutePath().toString());
        }
    }

    protected void copyFolder(Path source, Path destination) {
        try {
            Files.walk(source)
                    .map(Path::toFile)
                    .filter(file -> Files.isRegularFile(file.toPath()))
                    .forEach(file -> {
                        Path sourcePath = file.toPath();
                        Path relativePath = source.relativize(sourcePath);
                        Path destinationPath = destination.resolve(relativePath);

                        try {
                            Files.createDirectories(destinationPath);
                            Files.copy(sourcePath, destinationPath, REPLACE_EXISTING);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            System.out.println("EX: "+ex.getMessage());
                            throw new RuntimeException("FAILED TO COPY FILE: "+sourcePath.toAbsolutePath());
                        }
                    });
        } catch (IOException ex) {
            System.out.println("FAILED TO COPY "+source.toAbsolutePath().toString()+" TO "+destination.toAbsolutePath().toString());
        }
    }
}
