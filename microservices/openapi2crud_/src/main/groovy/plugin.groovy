import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

class OpenApi2CrudPlugin implements Plugin<Project> {

    @Override
    void apply(final Project project) {
        project.afterEvaluate {
            project.tasks.create('generateCrud', GenerateCrud)
        }
    }
    
}

class GenerateCrud extends DefaultTask {

    GenerateCrud() {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    void perform() {

        def input1 = prompt("tell me", "something")
        def input2 = prompt("tell me", "something else")
        System.out.println(input1)
        System.out.println(input2)
    }
    
    static String prompt(message, defaultValue = null) {
        def msg = "> $message" + (defaultValue ? " [$defaultValue]: " : ": ")

        def console = System.console()
        def input
        if (console) {
            input = console.readLine(msg)
        } else {
            println "$msg"
            Scanner scanner = new Scanner(System.in)
            input = scanner.nextLine()
        }
        return input.trim().isEmpty() ? String.valueOf(defaultValue) : input
    }
    
}
