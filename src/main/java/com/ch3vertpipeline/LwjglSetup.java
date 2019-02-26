package com.ch3vertpipeline;

import java.nio.IntBuffer;
import java.util.Scanner;
import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * TODO: rename shader extensions to glsl
 *  
 * @author ken
 */
public class LwjglSetup {

    private long window;
    private int vertex_shader;
    private int fragment_shader;
    private int tess_control_shader;
    private int tess_evaluation_shader;
    private int program;
    private int vertex_array_object;
    
    public LwjglSetup() {
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        // Configure GLFW
        glfwDefaultWindowHints(); // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

        // Create the window
        window = glfwCreateWindow(300, 300, "Hello World!", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
            }
        });

        // Get the thread stack and push a new frame
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window
            glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        } // the stack frame is popped automatically

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        // Enable v-sync
        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(window);
    }

    public void run() {
        System.out.println("Hello LWJGL " + Version.getVersion() + "!");
        init();
        loop();

        //TODO: Move this to a shutdown() method
        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    private void loop() {
        GL.createCapabilities();//Critical
        System.out.println("OpenGL Verion: " + glGetString(GL_VERSION));
        this.compileShader();
        vertex_array_object = glGenVertexArrays();
        glBindVertexArray(vertex_array_object);
        

        while (!glfwWindowShouldClose(window)) {
            double curTime = System.currentTimeMillis() / 1000.0;
            double slowerTime = curTime;//assigned direcly but I was applying a factor here
//            final float colour[] = {
//                (float) Math.sin(slowerTime) * 0.5f + 0.5f,
//                (float) Math.cos(slowerTime) * 0.5f + 0.5f,
//                0.0f, 1.0f};
            final float colour[] = {0.0f, 0.0f, 0.0f, 1.0f};

            glClearBufferfv(GL_COLOR, 0, colour);

            glUseProgram(program);

            final float attrib[] = {
                (float) Math.sin(slowerTime) * 0.5f,
                (float) Math.cos(slowerTime) * 0.6f,
                0.0f, 0.0f};

            //glPatchParameteri(GL_PATCH_VERTICES, 3);//this is the default so is unneeded
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
            glDrawArrays(GL_TRIANGLES, 0, 3);
            //glPointSize(5.0f);
            //glDrawArrays(GL_PATCHES, 0, 3);
            glVertexAttrib4fv(0, attrib);
            
            glfwSwapBuffers(window); // swap the color buffers
            glfwPollEvents();
        }
        glDeleteVertexArrays(vertex_array_object);
        glDeleteProgram(program);
    }

    private String readFileAsString(String filename) {
        String next = new Scanner(LwjglSetup.class.getResourceAsStream(filename), "UTF-8").useDelimiter("\\A").next();
        System.out.println("readFileAsString: " + next);
        return next;
    }

    //TODO: make this method part of the frameowork
    //TODO: have it return an int
    //TODO: add checks glGetShaderi(fs, GL_COMPILE_STATUS) for
    //both shaders to check compile
    //TODO: add check for link stages
    //TODO: creating shaders is _very_ repetitive can obviously fix this...
    //see following to do above: https://www.youtube.com/watch?v=q_dS3JuoeDw
    private void compileShader() {
        //int program;
        //NEW CODE
        //create and compile vertex shader
        String vertShaderSource = readFileAsString("/vert.glsl");
        vertex_shader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertex_shader, vertShaderSource);
        glCompileShader(vertex_shader);
        //check compilation
        if (glGetShaderi(vertex_shader, GL_COMPILE_STATUS) != 1) {
            System.err.println(glGetShaderInfoLog(vertex_shader));
            System.exit(1);
        }

        //create and compile fragment shader
        String fragShaderSource = readFileAsString("/frag.glsl");
        fragment_shader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragment_shader, fragShaderSource);
        glCompileShader(fragment_shader);
        //check compilation
        if (glGetShaderi(fragment_shader, GL_COMPILE_STATUS) != 1) {
            System.err.println(glGetShaderInfoLog(fragment_shader));
            System.exit(1);
        }

        //create and compile tessellation shader
        String tessControlShaderSource = readFileAsString("/control.tess.glsl");
        tess_control_shader = glCreateShader(GL40.GL_TESS_CONTROL_SHADER);
        glShaderSource(tess_control_shader, tessControlShaderSource);
        glCompileShader(tess_control_shader);
        //check compilation
        if (glGetShaderi(tess_control_shader, GL_COMPILE_STATUS) != 1) {
            System.err.println(glGetShaderInfoLog(tess_control_shader));
            System.exit(1);
        }

        //create and compile tessellation shader
        String tessEvaluationShaderSource = readFileAsString("/eval.tess.glsl");
        tess_evaluation_shader = glCreateShader(GL40.GL_TESS_EVALUATION_SHADER);
        glShaderSource(tess_evaluation_shader, tessEvaluationShaderSource);
        glCompileShader(tess_evaluation_shader);
        //check compilation
        if (glGetShaderi(tess_evaluation_shader, GL_COMPILE_STATUS) != 1) {
            System.err.println(glGetShaderInfoLog(tess_evaluation_shader));
            System.exit(1);
        }

        //create program and attach it
        program = glCreateProgram();
        glAttachShader(program, vertex_shader);
        glAttachShader(program, fragment_shader);
        glAttachShader(program, tess_control_shader);
        glAttachShader(program, tess_evaluation_shader);

        glLinkProgram(program);
        //check link       
        if (glGetProgrami(program, GL_LINK_STATUS) != 1) {
            System.err.println(glGetProgramInfoLog(program));
            System.exit(1);
        }
        glValidateProgram(program);
        if (glGetProgrami(program, GL_VALIDATE_STATUS) != 1) {
            System.err.println(glGetProgramInfoLog(program));
            System.exit(1);
        }
        //delete shaders as the program has them now
        glDeleteShader(vertex_shader);
        glDeleteShader(fragment_shader);
        glDeleteShader(tess_control_shader);
        glDeleteShader(tess_evaluation_shader);
        //return program;
    }
}
