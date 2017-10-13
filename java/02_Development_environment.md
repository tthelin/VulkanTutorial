In this chapter we'll set up your environment for developing Vulkan applications
and install some useful libraries. While you could develop using just the JDK,
this tutorial will also help you setup an Eclipse project for that IDE. Other
IDEs could also be used, simply adapt these instructions.

## Prequisites

If you haven't already, install a JDK supporting Java 8+ as well as your IDE /
editor of choice (although this tutorial only provides instructions for setting
up an Eclipse project).

* [JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
* [Eclipse](http://www.eclipse.org/downloads/eclipse-packages/)

## Vulkan SDK

The most important component you'll need for developing Vulkan applications is
the SDK. It includes the headers, standard validation layers, debugging tools
and a loader for the Vulkan functions. The loader looks up the functions in the
driver at runtime, similarly to GLEW for OpenGL - if you're familiar with that.

The SDK can be downloaded from [the LunarG website](https://vulkan.lunarg.com/)
using the buttons at the bottom of the page. You don't have to create an
account, but it will give you access to some additional documentation that may
be useful to you.

![](/images/vulkan_sdk_download_buttons.png)

### Windows

Proceed through the installation and pay attention to the install location of
the SDK. The first thing we'll do is verify that your graphics card and driver
properly support Vulkan. Go to the directory where you installed the SDK, open
the `Bin32` directory and run the `cube.exe` demo. You should see the following:

![](/images/cube_demo.png)

If you receive an error message then ensure that your drivers are up-to-date,
include the Vulkan runtime and that your graphics card is supported. See the
[introduction chapter](!Introduction) for links to drivers from the major
vendors.

There are two other programs in this directory that will be useful for
development. The `vkjson_info.exe` program generates a JSON file with a detailed
description of the capabilities of your hardware when using Vulkan. If you are
wondering what support is like for extensions and other optional features among
the graphics cards of your end users, then you can use [this website](http://vulkan.gpuinfo.org/)
to view the results of a wide range of GPUs.

The `glslangValidator.exe` program will be used to compile shaders from the
human-readable [GLSL](https://en.wikipedia.org/wiki/OpenGL_Shading_Language) to
bytecode. We'll cover this in depth in the [shader modules](!Drawing_a_triangle/Graphics_pipeline_basics/Shader_modules)
chapter. The `Bin32` directory also contains the binaries of the Vulkan loader
and the validation layers, while the `Lib32` directory contains the libraries.

The `Doc` directory contains useful information about the Vulkan SDK and an
offline version of the entire Vulkan specification. Lastly, there's the
`Include` directory that contains the Vulkan headers. Feel free to explore the
other files, but we won't need them for this tutorial.

### Linux

These instructions will be aimed at Ubuntu users, but you may be able to follow
along by compiling the LunarG SDK yourself and changing the `apt` commands to
the package manager commands that are appropriate for you.

If using C++, you should already have a version of GCC installed that supports
modern C++ (4.8 or later). You also need both CMake and make.

Open a terminal in the directory where you've downloaded the `.run` script, make
it executable and run it:

```bash
chmod +x vulkansdk-linux-x86_64-xxx.run
./vulkansdk-linux-x86_64-xxx.run
```

It will extract all of the files in the SDK to a `VulkanSDK` subdirectory in the
working directory. Move the `VulkanSDK` directory to a convenient place and take
note of its path. Open a terminal in the root directory of the SDK, which will
contain files like `build_examples.sh`.

The samples in the SDK and one of the libraries that you will later use for your
program depend on the XCB library. This is a C library that is used to interface
with the X Window System. It can be installed in Ubuntu from the `libxcb1-dev`
package. You also need the generic X development files that come with the
`xorg-dev` package.

```bash
sudo apt install libxcb1-dev xorg-dev
```

You can now build the Vulkan examples in the SDK by running:

```bash
./build_examples.sh
```

If compilation was successful, then you should now have a
`./examples/build/cube` executable. Run it from the `examples/build` directory
with `./cube` and ensure that you see the following pop up in a window:

![](/images/cube_demo_nowindow.png)

If you receive an error message then ensure that your drivers are up-to-date,
include the Vulkan runtime and that your graphics card is supported. See the
[introduction chapter](!Introduction) for links to drivers from the major
vendors.

## LWJGL + JOML

The [Lightweight Java Game Library](https://www.lwjgl.org/) (LWJGL) is how you
will access the various C APIs (Vulkan, GLFW) from Java. It's essentially
a bunch of independent "binding" modules (one for each API) and a base library
providing common support.

Go to https://www.lwjgl.org/download and pick what modules you want. At a
minimum you need:

* The windows native libraries
* LWJGL Core
* Vulkan bindings
* GLFW bindings
* JOML Addon

It's also useful to get the source and javadoc along with the above, so it's
recommended to check those boxes too.

Click on the download link, and extract the archive file somewhere convenient
as we'll need to point Eclipse to that path later.

### GLFW

As mentioned before, Vulkan by itself is a platform agnostic API and does not
include tools for creating a window to display the rendered results. To benefit
from the cross-platform advantages of Vulkan and to avoid the horrors of Win32,
we'll use the [GLFW library](http://www.glfw.org/) to create a window, which
supports both Windows and Linux. There are other libraries available for this
purpose, like [SDL](https://www.libsdl.org/), but the advantage of GLFW is that
it also abstracts away some of the other platform-specific things in Vulkan
besides just window creation.

LWJGL's GLFW binding library contains the native GLFW shared library so there's
nothing you have to do extra here besides make sure you checked GLFW when
downloading LWJGL.

### JOML

3D graphics development requires the use of linear algebra, and we'll use the
[Java OpenGL Math Library](https://github.com/JOML-CI/JOML) (JOML) to do it.

Even though "OpenGL" is in the name, it's really just a linear algebra library
and there's nothing about it that requires the use of OpenGL.

Like GLFW, it's easiest to get this if you just include it as part of the LWJGL
bundle you download.


## Setting up Eclipse

Now that you've installed all of the dependencies we can set up a basic Eclipse 
project for Vulkan and write a little bit of code to make sure that
everything works.

Start Eclipse, pick a workspace of your choice (or make a new one), and create
a new Java project named "VulkanTutorial" (or whatever you want)

![](/images/eclipse_new_project_menu.png)

![](/images/eclipse_new_project.png)

Create a new Java class in your project and name the class "VulkanTest" within
a package called "tutorial"

![](/images/eclipse_new_class_menu.png)

![](/images/eclipse_new_class.png)

Now add the following code to the file. Don't worry about trying to
understand it right now; we're just making sure that you can compile and run
Vulkan applications. We'll start from scratch in the next chapter.

```java
import static org.lwjgl.glfw.GLFW.GLFW_CLIENT_API;
import static org.lwjgl.glfw.GLFW.GLFW_NO_API;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.vkEnumerateInstanceExtensionProperties;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

public class VulkanTest {
  public static void main(String args[]) {
    glfwInit();

    glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
    long window = glfwCreateWindow(800, 600, "Vulkan window", 0, 0);

    try (MemoryStack stack = stackPush()) {
      IntBuffer extensionCountBuffer = stack.mallocInt(1);
      vkEnumerateInstanceExtensionProperties((ByteBuffer) null, extensionCountBuffer, null);
      System.out.println(String.format("%d extensions supported", extensionCountBuffer.get()));
    }
    Vector4f vec = new Vector4f();
    Matrix4f matrix = new Matrix4f();
    @SuppressWarnings("unused")
    Vector4f test = matrix.transform(vec);

    while (!glfwWindowShouldClose(window)) {
      glfwPollEvents();
    }

    glfwDestroyWindow(window);

    glfwTerminate();
  }
}
```

Let's now configure the project to get rid of the errors. At its heart, you need
need to add the LWJGL and JOGL jar files you downloaded earlier as
dependencies. While you could do this directly, it will make your life easier
in the future if you setup a "User Library" for each one in your workspace,
and then make this (and any future) project reference these new User Libraries.

If you already know how to setup User Libraries in Eclipse, you can skip this
next part

To make a User Library, open your workspaces preferences, go to the
"User Library" item of the Java Build Path, and click "New".

![](/images/eclipse_workspace_preferences_menu.png)

![](/images/eclipse_new_user_library_button.png)

Type in "LWJGL" as the name and click "OK".

![](/images/eclipse_new_user_library.png)

Now click the "Add External Jars..." button on the right, and locate where you
unzipped the LWJGL jar files you downloaded earlier.

Specifically select the following jar files:

* lwjgl.jar
* lwjgl-natives-windows.jar
* lwjgl-vulkan.jar
* lwjgl-glfw.jar
* lwjgl-glfw-natives-windows.jar

If you downloaded other modules you can select those too if you wish, however do
not select anything with the word "sources" or "javadoc" in the name, as that
will be dealt with next.

Your User Libraries window should look something like this (order doesn't
matter):

![](/images/eclipse_new_user_library_base_jars.png)

Now that we have the implementation jars referenced, we can add in the source
and javadoc jars, which should speed up your development. For "lwjgl.jar",
"lwjgl-glfw.jar", and "lwjgl-vulkan.jar" do the following:

1. Add in the source jar
    1. Select the "Source attachment:" item for the jar
    2. Click "Edit..." on the right
    3. Select the "External Location" option
    4. Click the "External File..." button
    5. Find the jar file named like the one you're editing this for, but with
       "sources" in the name.
    6. Click "OK"
2. Add in the javadoc jar
    1. Select the "Javadoc location:" item for the jar
    2. Click "Edit..." on the right
    3. Select the "Javadoc in archive" option
    4. Make sure "External file" is checked (it's the default)
    5. Click the "Browse..." button next to the "Archive Path" text box
    6. Find the jar file named like the one you're editing this for, but with
       "javadoc" in the name.
    7. Click "OK"
	
You do not need to edit the entries that have "natives" in the name; those are
special jar files that hold the DLLs needed to interface with Windows and
various APIs.

Your "User Libraries" window should now look something like the following:

![](/images/eclipse_new_user_library_all_jars.png)

Now make an additional User Library, but for JOML. You only need to initially
select "joml.jar", and then add in its javadoc and source jars.

It should look something like this:

![](/images/eclipse_new_user_library_joml.png)

With defining User Libraries done, we can now connect those two libraries to our
project to get rid of all of our build errors.

Open your project's properties.

![](/images/eclipse_project_propeties_menu.png)

And select "Java Build Path" on the left.

![](/images/eclipse_java_build_path.png)

Click "Add Library" on the right, select "User Library" in this list, and click
"Next".

![](/images/eclipse_add_library.png

Select both JOML and LWJGL and click "Finish".

![](/images/eclipse_library_select.png)

Your build path settings should look similar to this:

![](/images/eclipse_java_build_path_finished.png)

Now click "OK". All of your build errors should go away, and there should be
no warnings.

Press `Ctrl-F11` to compile and run the project and you should see a window pop
up like this:

![](/images/eclipse_test_window.png)

Additionally, the eclipse console should print out a non-zero numbe of
extensions.

![](/images/eclipse_console_output.png)

Congratulations, you're all set for playing with Vulkan!

You are now all set for [the real adventure](!Drawing_a_triangle/Setup/Base_code).
