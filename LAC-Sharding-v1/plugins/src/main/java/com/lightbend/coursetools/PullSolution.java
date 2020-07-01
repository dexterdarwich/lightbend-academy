package com.lightbend.coursetools;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Mojo(name = "pullSolution", defaultPhase = LifecyclePhase.INITIALIZE)
public class PullSolution extends NavigationBase {

    public void execute() {
        Path solution = getSolutionDirectory();

        if(solution != null) {
            Path mainSource = solution.resolve(mainPath);
            Path mainDestination = Paths.get(mainPath);

            System.out.println("PULLING SOLUTION FOR "+solution.getFileName());
            System.out.println("SOURCE: "+mainSource.toAbsolutePath().toString());
            System.out.println("DESTINATION: "+mainDestination.toAbsolutePath().toString());

            purgeFolder(mainDestination);
            copyFolder(mainSource, mainDestination);
        } else {
            System.out.println("EXERCISE NOT FOUND");
        }
    }
}
