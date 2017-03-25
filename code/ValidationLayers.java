
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
import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.EXTDebugReport.VK_DEBUG_REPORT_ERROR_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugReport.VK_DEBUG_REPORT_WARNING_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugReport.VK_EXT_DEBUG_REPORT_EXTENSION_NAME;
import static org.lwjgl.vulkan.EXTDebugReport.VK_STRUCTURE_TYPE_DEBUG_REPORT_CALLBACK_CREATE_INFO_EXT;
import static org.lwjgl.vulkan.EXTDebugReport.vkCreateDebugReportCallbackEXT;
import static org.lwjgl.vulkan.EXTDebugReport.vkDestroyDebugReportCallbackEXT;
import static org.lwjgl.vulkan.VK10.VK_API_VERSION_1_0;
import static org.lwjgl.vulkan.VK10.VK_MAKE_VERSION;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.vkCreateInstance;
import static org.lwjgl.vulkan.VK10.vkDestroyInstance;
import static org.lwjgl.vulkan.VK10.vkEnumerateInstanceLayerProperties;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkDebugReportCallbackCreateInfoEXT;
import org.lwjgl.vulkan.VkDebugReportCallbackEXT;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkLayerProperties;

public class ValidationLayers {
  private static final int WIDTH = 800;
  private static final int HEIGHT = 600;
  private static final boolean ENABLE_VALIDATION_LAYERS = true;

  private static final String[] VALIDATION_LAYER_NAMES = {
    "VK_LAYER_LUNARG_standard_validation",
  };

  private static final VkDebugReportCallbackEXT DEBUG_CALLBACK =
      new VkDebugReportCallbackEXT() {
        @Override
        public int invoke(
            int flags,
            int objectType,
            long object,
            long location,
            int messageCode,
            long pLayerPrefix,
            long pMessage,
            long pUserData) {
          System.err.println("Validation layer: " + VkDebugReportCallbackEXT.getString(pMessage));
          return 0;
        }
      };

  private long window;
  private VkInstance instance;
  private long debugCallbackHandle;

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
    debugCallbackHandle = setupDebugCallback();
  }

  private void mainLoop() {
    while (!glfwWindowShouldClose(window)) {
      glfwPollEvents();
    }
  }

  private void cleanup() {
    if (debugCallbackHandle != 0) {
      vkDestroyDebugReportCallbackEXT(instance, debugCallbackHandle, null);
      debugCallbackHandle = 0;
    }
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

  private VkInstance createInstance() {
    if (ENABLE_VALIDATION_LAYERS && !checkValidationLayerSupport()) {
      throw new AssertionError("Validation layers requested, but not available!");
    }

    try (MemoryStack stack = stackPush()) {
      VkApplicationInfo appInfo =
          VkApplicationInfo.callocStack(stack)
              .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
              .pApplicationName(stack.UTF8("Hello Triangle"))
              .applicationVersion(VK_MAKE_VERSION(1, 0, 0))
              .pEngineName(stack.UTF8("No Engine"))
              .engineVersion(VK_MAKE_VERSION(1, 0, 0))
              .apiVersion(VK_API_VERSION_1_0);

      PointerBuffer extensions = getRequiredExtensions();
      VkInstanceCreateInfo createInfo =
          VkInstanceCreateInfo.callocStack(stack)
              .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
              .pApplicationInfo(appInfo)
              .ppEnabledExtensionNames(extensions);

      if (ENABLE_VALIDATION_LAYERS) {
        PointerBuffer layerNames = stack.mallocPointer(VALIDATION_LAYER_NAMES.length);
        for (String name : VALIDATION_LAYER_NAMES) {
          layerNames.put(stack.UTF8(name));
        }
        layerNames.flip();
        createInfo.ppEnabledLayerNames(layerNames);
      }

      PointerBuffer pInstance = stack.callocPointer(1);
      int err = vkCreateInstance(createInfo, null, pInstance);
      if (err != VK_SUCCESS) {
        throw new AssertionError("Failed to create VkInstance: " + err);
      }
      return new VkInstance(pInstance.get(), createInfo);
    }
  }

  private long setupDebugCallback() {
    if (!ENABLE_VALIDATION_LAYERS) {
      return 0;
    }

    try (MemoryStack stack = stackPush()) {
      VkDebugReportCallbackCreateInfoEXT createInfo =
          VkDebugReportCallbackCreateInfoEXT.callocStack(stack)
              .sType(VK_STRUCTURE_TYPE_DEBUG_REPORT_CALLBACK_CREATE_INFO_EXT)
              .flags(VK_DEBUG_REPORT_ERROR_BIT_EXT | VK_DEBUG_REPORT_WARNING_BIT_EXT)
              .pfnCallback(DEBUG_CALLBACK);
      LongBuffer callbackHandleBuffer = stack.mallocLong(1);
      int err = vkCreateDebugReportCallbackEXT(instance, createInfo, null, callbackHandleBuffer);
      if (err != VK_SUCCESS) {
        throw new AssertionError("Failed to setup debug callback: " + err);
      }
      return callbackHandleBuffer.get();
    }
  }

  private PointerBuffer getRequiredExtensions() {
    PointerBuffer requiredExtensions = glfwGetRequiredInstanceExtensions();
    if (requiredExtensions == null) {
      throw new AssertionError("Failed to find list of required Vulkan extensions");
    }

    // Use the current memory stack as an autorelease pool
    PointerBuffer extensions =
        stackGet()
            .mallocPointer(requiredExtensions.remaining() + (ENABLE_VALIDATION_LAYERS ? 1 : 0));
    extensions.put(requiredExtensions);
    if (ENABLE_VALIDATION_LAYERS) {
      extensions.put(stackGet().UTF8(VK_EXT_DEBUG_REPORT_EXTENSION_NAME));
    }
    extensions.flip();
    return extensions;
  }

  private boolean checkValidationLayerSupport() {
    try (MemoryStack stack = stackPush()) {
      IntBuffer layerCountBuffer = stack.mallocInt(1);
      vkEnumerateInstanceLayerProperties(layerCountBuffer, null);
      int layerCount = layerCountBuffer.get(0);

      VkLayerProperties.Buffer availableLayers = VkLayerProperties.callocStack(layerCount);
      vkEnumerateInstanceLayerProperties(layerCountBuffer, availableLayers);

      Set<String> foundLayers = new HashSet<>();
      for (int i = 0; i < layerCount; i++) {
        foundLayers.add(availableLayers.get().layerNameString());
      }

      Set<String> desiredLayers = new HashSet<>();
      Collections.addAll(desiredLayers, VALIDATION_LAYER_NAMES);

      return foundLayers.containsAll(desiredLayers);
    }
  }

  public static void main(String args[]) {
    new ValidationLayers().run();
  }
}
