package assertionFinder;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;
import spoon.support.compiler.FileSystemFolder;

public class FindAssert {

    public static void main(String[] args) {
        // Get the directory containing the projects
    	String workingDir = System.getProperty("user.dir");
    	String repoDirLoc = workingDir + "/../repos";
    	System.out.println("Looking for projects in: " + repoDirLoc + "...");

        File reposDir = new File(repoDirLoc);
        File[] projects = reposDir.listFiles();
//        System.out.println("Found the following projects:");
//        for (File project : projects) {
//            System.out.println(project.getName());
//        }
        if (projects == null) {
        	System.err.println("No projects found in the 'repos' directory.\n");
        	return;
        }
        System.out.println("Found " + projects.length + " projects.\n");

        // Analyze each project
        for (File project : projects) {
            try {
                if (project.isDirectory()) {
                    analyzeProject(project);
                }
            } catch (spoon.JLSViolation e) {
                System.err.println("Error analyzing project " + project.getName() + ": " + e.getMessage());
                // Continue with the next project
            }
        }
    }

    private static void analyzeProject(File projectDir) {
        // Init spoon stuff
        Launcher launcher = new Launcher();
        launcher.addInputResource(new FileSystemFolder(projectDir));
        launcher.getEnvironment().setNoClasspath(true);

        // Build the model
        try {
            launcher.buildModel();
        } catch (spoon.compiler.ModelBuildingException e) {
            System.err.println("Error building model for project " + projectDir.getName() + ": " + e.getMessage());
            return;  // Skip this project and continue with the next one
        }

        CtModel model = launcher.getModel();

        List<CtMethod<?>> testMethods = new ArrayList<>();
        List<CtMethod<?>> allMethods = new ArrayList<>();
        
        String[] projectName = projectDir.toString().split("/");
        System.out.println("\n----- Analyzing repo: " + projectName[projectName.length-1]  + "... -----\n");
        
        // Iterate over all classes in the model
        for (CtType<?> type : model.getAllTypes()) {
            if (type instanceof CtClass) {
                CtClass<?> ctClass = (CtClass<?>) type;

                for (CtMethod<?> method : ctClass.getMethods()) {
                    if (method.getAnnotations().stream().anyMatch(annotation -> annotation.getAnnotationType().getSimpleName().equals("Test"))) {
                        testMethods.add(method);
                    } else {
                        allMethods.add(method);
                    }
                }
            }
        }

        // Find matching focal methods
        findMatchingFocalMethods(testMethods, allMethods);
    }

    // Check if the method contains an invocation to a method in the Assert class
    private static boolean containsAssertStatement(CtMethod<?> method) {
        return method.getElements(e -> e instanceof CtInvocation)
                .stream()
                .map(e -> (CtInvocation<?>) e)
                .anyMatch(invocation -> {
                    CtExecutableReference<?> executable = invocation.getExecutable();
                    CtTypeReference<?> declaringType = executable.getDeclaringType();
                    return declaringType != null && declaringType.getSimpleName().equals("Assert");
                });
    }
    
    private static Set<String> findMatchingFocalMethods(List<CtMethod<?>> testMethods, List<CtMethod<?>> allMethods) {
        Set<String> matchingFocalMethods = new HashSet<>();

        // Iterate over all test methods
        for (CtMethod<?> testMethod : testMethods) {
            if (containsAssertStatement(testMethod)) {
                System.out.println("\nLocated test method: " + testMethod.getSignature() + ":");

                // Get all method calls within the test method
                List<CtInvocation> invocations = testMethod.getElements(new TypeFilter<>(CtInvocation.class));
                System.out.println("  Invocates: ");

                int assertCount = 0;
                for (CtInvocation<?> invocation : invocations) {
                    String signature = invocation.getExecutable().getSignature();
                    System.out.println("    " + signature);

                    // Check if the invocation is an assert statement
                    if (signature.startsWith("assert")) {
                        assertCount++;

                        // Print the arguments passed to the assert statement
                        System.out.println("    Arguments: ");
                        for (CtExpression<?> argument : invocation.getArguments()) {
                            System.out.println("      " + argument);

                            // If the argument is a method call, print it as the function being tested
                            if (argument instanceof CtInvocation) {
                                System.out.println("      Function being tested: " + ((CtInvocation<?>) argument).getExecutable().getSignature());
                            }
                        }
                    }
                }

                System.out.println("  Number of asserts: " + assertCount);
            }
        }


        return matchingFocalMethods;
    }

}