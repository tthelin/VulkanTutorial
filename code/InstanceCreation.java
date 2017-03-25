
import static org.lwjgl.glfw.GLFW.GLFW_CLIENT_API;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_NO_API;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.VK_API_VERSION_1_0;
import static org.lwjgl.vulkan.VK10.VK_MAKE_VERSION;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.vkCreateInstance;
import static org.lwjgl.vulkan.VK10.vkDestroyInstance;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;

public class InstanceCreation {
  private static final int WIDTH = 800;
  private static final int HEIGHT = 600;

  private long window;
  private VkInstance instance;

  private void run() {
    try {
      initWindow();
      initVulkan();
      mainLoop();
    } finally {
      cleanup();
    }
  }

  private void initWindow() {
    glfwInit();

    glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
    glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

    window = glfwCreateWindow(WIDTH, HEIGHT, "Vulkan", 0, 0);
  }

  private void initVulkan() {
    if (!glfwVulkanSupported()) {
      throw new AssertionError("Vulkan not supported");
    }
    instance = createInstance();
  }

  private void mainLoop() {
    while (!glfwWindowShouldClose(window)) {
      glfwPollEvents();
    }
  }

  private void cleanup() {
    if (instance != null) {
      vkDestroyInstance(instance, null);
      instance = null;
    }
    if (window != 0) {
      glfwDestroyWindow(window);
      window = 0;
    }
    glfwTerminate();
  }

  private static VkInstance createInstance() {
    try (MemoryStack stack = stackPush()) {
      VkApplicationInfo appInfo =
          VkApplicationInfo.callocStack(stack)
              .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
              .pApplicationName(stack.ASCII("Hello Triangle"))
              .applicationVersion(VK_MAKE_VERSION(1, 0, 0))
              .pEngineName(stack.ASCII("No Engine"))
              .engineVersion(VK_MAKE_VERSION(1, 0, 0))
              .apiVersion(VK_API_VERSION_1_0);

      PointerBuffer extensions = glfwGetRequiredInstanceExtensions();
      if (extensions == null) {
        throw new AssertionError("Failed to find list of required Vulkan extensions");
      }
      VkInstanceCreateInfo createInfo =
          VkInstanceCreateInfo.callocStack(stack)
              .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
              .pApplicationInfo(appInfo)
              .ppEnabledExtensionNames(extensions);

      PointerBuffer pInstance = stack.callocPointer(1);
      int err = vkCreateInstance(createInfo, null, pInstance);
      if (err != VK_SUCCESS) {
        throw new AssertionError("Failed to create VkInstance: " + err);
      }
      return new VkInstance(pInstance.get(), createInfo);
    }
  }

  public static void main(String args[]) {
    new InstanceCreation().run();
  }
}
