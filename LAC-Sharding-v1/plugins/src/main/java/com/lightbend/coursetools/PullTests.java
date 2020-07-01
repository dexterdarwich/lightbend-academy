package com.lightbend.coursetools;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Mojo(name = "pullTests", defaultPhase = LifecyclePhase.INITIALIZE)
public class PullTests extends NavigationBase {

    public void execute() {
        Path solution = getSolutionDirectory();

        if(solution != null) {
            Path testSource = solution.resolve(testPath);
            Path testDestination = Paths.get(testPath);

            System.out.println("PULLING TESTS FOR "+solution.getFileName());
            System.out.println("SOURCE: "+testSource.toAbsolutePath().toString());
            System.out.println("DESTINATION: "+testDestination.toAbsolutePath().toString());

            purgeFolder(testDestination);
            copyFolder(testSource, testDestination);
        } else {
            System.out.println("EXERCISE NOT FOUND");
        }
    }
}
