## General structure

In the previous chapter you've created a Vulkan project with all of the proper
configuration and tested it with the sample code. In this chapter we're starting
from scratch with the following code:

```java
public class BaseCode {

  private void run() {
    try {
      initVulkan();
      mainLoop();
    } finally {
      cleanup();
    }
  }

  private void initVulkan() {
  }

  private void mainLoop() {
  }

  private void cleanup() {
  }

  public static void main(String args[]) {
    new BaseCode().run();
  }
}
```

The program itself is wrapped into a class where we'll store the Vulkan objects
as private members and add methods to initiate each of them, which will
be called from the `initVulkan` function. Once everything has been prepared, we
enter the main loop to start rendering frames. We'll fill in the `mainLoop`
function to include a loop that iterates until the window is closed in a moment.
Once the window is closed and `mainLoop` returns, we'll make sure to deallocate
the resources we've used in the `cleanup` function.

If any kind of fatal error occurs during execution then we'll throw an
AssertionError with a descriptive message, which will cause our program to exit
with the error printed on the screen. One example of an error that we will deal
with soon is finding out that a certain required extension is not supported.

Roughly every chapter that follows after this one will add one new function that
will be called from `initVulkan` and one or more new Vulkan objects to the
private members that need to be disposed of at the end in `cleanup`.

## Resource management

### Native objects and handles

Vulkan objects are either created directly with functions like `vkCreateXXX`, or
allocated through another object with functions like `vkAllocateXXX`. After
making sure that an object is no longer used anywhere, you need to destroy it
with the counterparts `vkDestroyXXX` and `vkFreeXXX`. The parameters for these
functions generally vary for different types of objects, but there is one
parameter that they all share: `pAllocator`. This is an optional parameter that
allows you to specify callbacks for a custom memory allocator. We will ignore
this parameter in the tutorial and always pass `null` as argument.
 
When using Vulkan with Java, the `vkCreateXXX` and `vkAllocateXXX` calls return
a long, which represents a reference to a "native-to-Vulkan object". The
reference itself is referred to as a "handle", and you pass the handle to
various Vulkan methods to tell it what native object to operate on.

Please don't confuse these "native objects" (as this tutorial will now refer to
them) with Java objects, as they are not the same thing. Java objects are
instances of Java classes. Native objects are just a logical construct of the
API you're calling that's referenced via a handle.

In LWGL, some important native objects have wrapper Java classes to make things
more type-safe to use, such as the VkInstance class. You make them by passing
the handle to its constructor, and some of the wrapped Vulkan API calls require
this special Java class instead of the naked handle to make sure you're passing
the right 64-bit handle value.

When the native objects are no longer needed, it's your job to call the
appropriate "destroy" function on the API to clean up the resources backing that
native object. As the JVM is unaware of these resources, the garbage collector
can't do this automatically so it's up to you.

To accomplish this, the code in this tutorial uses two strategies:

* If the native object needs to be alive during the entire program, its handle
  is saved off as a class member and the `cleanup` method will destroy it.
* If the native object only needs to be alive during a limited scope (such as
  a single method call), then the end of that scope needs an explicit destroy
  call, typically within a `finally` clause to make sure it happens on success
  or error.

### Data passing to/from Vulkan

Vulkan assumes we have direct control over the memory layout of any data passed
to it or received from it. Unfortunately since Java has no concept of structs
(which would have let the compiler do this for us), the only way you can do this
in Java is to make buffers and write/read them in a layout concious way.

At first glance it would seem like Java's NIO Buffers should be used for this,
however they have two downsides:

* Java doesn't actually know if Vulkan is still using the buffer and could clean
  it up before Vulkan is done using it (i.e. a program crash).
* It brings unpredictability to how much RAM you're using at any one point of
  time (since GC isn't under your control)
  
Due to this, LWJGL's Vulkan bindings generally want you to pass in buffers of
raw memory that you're directly controlling the lifecycle of. To help with this,
it provides a few Java classes:

* `MemoryUtil` - provides access to a typical heap via `malloc`, `calloc`, and
  `free`. It also has other utility methods like converting Java strings into
   UTF8 byte buffers.
* `MemoryStack` - Provides a "C stack like" way of allocating memory, where it's
  actually one giant buffer of raw memory under the hood and every time you
  allocate memory from it it's just shifting an offset to where the available
  memory now starts. Every time you enter a scope (method), you push a new
  frame onto the stack, and when you leave that scope (method) you pop it, thus
  `free`ing all allocated memory in that scope. While this simulates a C stack,
  because it's not actually on the Java stack it feels more like stackable
  "memory pools" where you can trivially free up an entire pool in one shoot.
  Regardless of the name, this class is extremely useful and makes managing
  buffers way easier as it handles the `free`ing for you (as long as you
  remember to pop it).

The actual Vulkan binding signatures take a combination of types that are fed
from the above.

* NIO Types
    * ByteBuffer (typically used when passing strings)
    * IntBuffer (for counts / sizes)
* CustomBuffer subclasses
    * PointerBuffer (for native object handles)
	* Subclasses of `Struct` (every Vulkan struct has a Java class named the
	  same extending `Struct`)

To make



When you only need to pass something simple to Vulkan that aleady exists via
Java NIO's Buffer class hierarchy, LWJGL uses that (such )

There are two sources of data buffers you could use to pass / get back data
from Vulkan

* [NIO Direct byte buffers](https://docs.oracle.com/javase/8/docs/api/java/nio/ByteBuffer.html)
* Raw memory

The advantage of using the first is that Java's garbage collector is aware of
it and can clean it up for you when you're no longer using it. The disadvantage
of using direct byte buffers is that ) and
. So you can get away
with using direct byte buffers if you're very careful, but it's actually easier
to just use raw memory and avoid both of those issues.

Obviously the biggest downside to using raw memory is that you're now
responsible for cleaning it up when its no longer needed, but LWJGL tries to
help out here with tools like MemoryStack.





LWJGL also chose to focus on using raw memory, so the Vulkan bindings typexpect

#### Lifecycle

LWJGL brings back C memory management for these raw memory buffers

Because we're dealing with a C API that cares about memory layout, and Java has
no built in support for that, we're forced to do the layout ourselves (via the
"structs" above), and unfortuantely to do that it means we have to deal with
managing raw memory.

LWJGL tries to make this easy by providing the following methods on all structs:

* `create()` - Allocate a Java NIO buffer for exactly one of these structs and
   make a new struct instance wrapped around it. Java's GC will clean up this
   memory.
* `malloc()` - Allocate raw memory for exactly one of these structs and
   make a new struct instance wrapped around it. You must free this memory
   manually.
* `calloc()` - `malloc()` followed by initializing the contents to all 0s.

For each of the above, there's a version that takes an `int` parameter and
returns a `Buffer` version of the struct. This flavor represents a C array of
these structs where they are laid out contiguously in memory.

There's also a "stack" version of each of the `malloc` and `calloc`
calls that doesn't use the normal heap but a special `MemoryStack` (more on this
later) that makes it easier to manage when to free the memory.
   
All structs and struct buffers implement AutoCloseable so they can be used in
try resource blocks to free the memory once you're done using it. Otherwise,
unless you're using MemoryStack, you need to free the memory explicitly yourself
using `free()` on the struct.

For when you need to have a buffer of memory to use with Vulkan that's not an
official struct, you can use `MemUtils` to `malloc`, `calloc`, and `free` as
needed (under the hood the structs use `MemUtils` to manage their memory).

#### Layout


## Integrating GLFW

Vulkan works perfectly fine without a creating a window if you want to use it
off-screen rendering, but it's a lot more exciting to actually show something!
First replace the `#include <vulkan/vulkan.h>` line with

```c++
#define GLFW_INCLUDE_VULKAN
#include <GLFW/glfw3.h>
```

That way GLFW will include its own definitions and automatically load the Vulkan
header with it. Add a `initWindow` function and add a call to it from the `run`
function before the other calls. We'll use that function to initialize GLFW and
create a window.

```c++
void run() {
    initWindow();
    initVulkan();
    mainLoop();
}

private:
    void initWindow() {

    }
```

The very first call in `initWindow` should be `glfwInit()`, which initializes
the GLFW library. Because GLFW was originally designed to create an OpenGL
context, we need to tell it to not create an OpenGL context with a subsequent
call:

```c++
glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
```

Because handling resized windows takes special care that we'll look into later,
disable it for now with another window hint call:

```c++
glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
```

All that's left now is creating the actual window. Add a `GLFWwindow* window;`
private class member to store a reference to it and initialize the window with:

```c++
window = glfwCreateWindow(800, 600, "Vulkan", nullptr, nullptr);
```

The first three parameters specify the width, height and title of the window.
The fourth parameter allows you to optionally specify a monitor to open the
window on and the last parameter is only relevant to OpenGL.

It's a good idea to use constants instead of hardcoded width and height numbers
because we'll be referring to these values a couple of times in the future. I've
added the following lines above the `HelloTriangleApplication` class definition:

```c++
const int WIDTH = 800;
const int HEIGHT = 600;
```

and replaced the window creation call with

```c++
window = glfwCreateWindow(WIDTH, HEIGHT, "Vulkan", nullptr, nullptr);
```

You should now have a `initWindow` function that looks like this:

```c++
void initWindow() {
    glfwInit();

    glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
    glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

    window = glfwCreateWindow(WIDTH, HEIGHT, "Vulkan", nullptr, nullptr);
}
```

To keep the application running until either an error occurs or the window is
closed, we need to add an event loop to the `mainLoop` function as follows:

```c++
void mainLoop() {
    while (!glfwWindowShouldClose(window)) {
        glfwPollEvents();
    }

    glfwDestroyWindow(window);

    glfwTerminate();
}
```

This code should be fairly self-explanatory. It loops and checks for events like
pressing the X button until the window has been closed by the user. This is also
the loop where we'll later call a function to render a single frame. Once the
window is closed, we need to clean up resources by destroying it and GLFW]
itself.

When you run the program now you should see a window titled `Vulkan` show up
until the application is terminated by closing the window. Now that we have the
skeleton for the Vulkan application, let's [create the first Vulkan object](!Drawing_a_triangle/Setup/Instance)!

[C++ code](/code/base_code.cpp)
