
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
import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;
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
import static org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR;
import static org.lwjgl.vulkan.VK10.VK_API_VERSION_1_0;
import static org.lwjgl.vulkan.VK10.VK_MAKE_VERSION;
import static org.lwjgl.vulkan.VK10.VK_QUEUE_GRAPHICS_BIT;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.VK_TRUE;
import static org.lwjgl.vulkan.VK10.vkCreateDevice;
import static org.lwjgl.vulkan.VK10.vkCreateInstance;
import static org.lwjgl.vulkan.VK10.vkDestroyDevice;
import static org.lwjgl.vulkan.VK10.vkDestroyInstance;
import static org.lwjgl.vulkan.VK10.vkEnumerateInstanceLayerProperties;
import static org.lwjgl.vulkan.VK10.vkEnumeratePhysicalDevices;
import static org.lwjgl.vulkan.VK10.vkGetDeviceQueue;
import static org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceQueueFamilyProperties;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkDebugReportCallbackCreateInfoEXT;
import org.lwjgl.vulkan.VkDebugReportCallbackEXT;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkLayerProperties;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkQueueFamilyProperties;

public class WindowSurface {
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

  private static class QueueFamilyIndices {
    private Optional<Integer> graphicsFamilyIndex;
    private Optional<Integer> presentFamilyIndex;

    public void setGraphicsFamilyIndex(int index) {
      graphicsFamilyIndex = Optional.of(index);
    }

    public void setPresentFamilyIndex(int index) {
      presentFamilyIndex = Optional.of(index);
    }

    public int getGraphicsFamilyIndex() {
      return graphicsFamilyIndex.get();
    }

    public int getPresentFamilyIndex() {
      return presentFamilyIndex.get();
    }

    public boolean isComplete() {
      return graphicsFamilyIndex.isPresent() && presentFamilyIndex.isPresent();
    }
  }

  private long window;
  private VkInstance instance;
  private long debugCallbackHandle;
  private long surface;

  private VkPhysicalDevice physicalDevice;
  private QueueFamilyIndices queueIndices;
  private VkDevice logicalDevice;

  @SuppressWarnings("unused") // for now
  private VkQueue graphicsQueue;

  @SuppressWarnings("unused") // For now
  private VkQueue presentQueue;

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
    debugCallbackHandle = setupDebugCallback(instance);
    surface = createSurface(instance, window);
    physicalDevice = pickPhysicalDevice(instance, surface);
    queueIndices = findQueueFamilies(physicalDevice, surface);
    logicalDevice = createLogicalDevice(physicalDevice, queueIndices);
    graphicsQueue = createGraphicsQueue(logicalDevice, queueIndices);
    presentQueue = createPresentQueue(logicalDevice, queueIndices);
  }

  private void mainLoop() {
    while (!glfwWindowShouldClose(window)) {
      glfwPollEvents();
    }
  }

  private void cleanup() {
    if (logicalDevice != null) {
      vkDestroyDevice(logicalDevice, null);
      logicalDevice = null;
    }
    if (surface != 0) {
      vkDestroySurfaceKHR(instance, surface, null);
      surface = 0;
    }
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

  private static VkInstance createInstance() {
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

      PointerBuffer instanceHandleBuffer = stack.mallocPointer(1);
      int err = vkCreateInstance(createInfo, null, instanceHandleBuffer);
      if (err != VK_SUCCESS) {
        throw new AssertionError("Failed to create VkInstance: " + err);
      }
      return new VkInstance(instanceHandleBuffer.get(), createInfo);
    }
  }

  private static long setupDebugCallback(VkInstance instance) {
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

  private static long createSurface(VkInstance instance, long window) {
    try (MemoryStack stack = stackPush()) {
      LongBuffer surfaceHandleBuffer = stack.mallocLong(1);
      int err = glfwCreateWindowSurface(instance, window, null, surfaceHandleBuffer);
      if (err != VK_SUCCESS) {
        throw new AssertionError("Failed to create window surface: " + err);
      }
      return surfaceHandleBuffer.get();
    }
  }

  private static VkPhysicalDevice pickPhysicalDevice(VkInstance instance, long surface) {
    try (MemoryStack stack = stackPush()) {
      IntBuffer deviceCountBuffer = stack.mallocInt(1);
      vkEnumeratePhysicalDevices(instance, deviceCountBuffer, null);
      int deviceCount = deviceCountBuffer.get(0);

      if (deviceCount == 0) {
        throw new AssertionError("Failed to find GPUs with Vulkan support!");
      }

      PointerBuffer devicePointers = stack.mallocPointer(deviceCount);
      vkEnumeratePhysicalDevices(instance, deviceCountBuffer, devicePointers);

      VkPhysicalDevice physicalDevice = null;
      for (int i = 0; i < deviceCount; ++i) {
        VkPhysicalDevice device = new VkPhysicalDevice(devicePointers.get(i), instance);
        if (isDeviceSuitable(device, surface)) {
          physicalDevice = device;
          break;
        }
      }

      if (physicalDevice == null) {
        throw new AssertionError("Failed to find suiteable GPU!");
      }
      return physicalDevice;
    }
  }

  private static VkDevice createLogicalDevice(
      VkPhysicalDevice physicalDevice, QueueFamilyIndices indices) {
    try (MemoryStack stack = stackPush()) {
      FloatBuffer queuePriorityBuffer = stack.mallocFloat(1).put(1.0f);
      queuePriorityBuffer.flip();

      Set<Integer> uniqueQueueFamilies = new HashSet<>();
      uniqueQueueFamilies.add(indices.getGraphicsFamilyIndex());
      uniqueQueueFamilies.add(indices.getPresentFamilyIndex());

      VkDeviceQueueCreateInfo.Buffer queueCreateInfos =
          VkDeviceQueueCreateInfo.callocStack(uniqueQueueFamilies.size(), stack);
      for (int index : uniqueQueueFamilies) {
        VkDeviceQueueCreateInfo queueCreateInfo = queueCreateInfos.get();
        queueCreateInfo
            .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
            .queueFamilyIndex(index)
            .pQueuePriorities(queuePriorityBuffer);
      }
      queueCreateInfos.rewind();

      VkDeviceCreateInfo createInfo =
          VkDeviceCreateInfo.callocStack(stack)
              .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
              .pQueueCreateInfos(queueCreateInfos);

      if (ENABLE_VALIDATION_LAYERS) {
        PointerBuffer layerNames = stack.mallocPointer(VALIDATION_LAYER_NAMES.length);
        for (String name : VALIDATION_LAYER_NAMES) {
          layerNames.put(stack.UTF8(name));
        }
        layerNames.flip();
        createInfo.ppEnabledLayerNames(layerNames);
      }

      PointerBuffer deviceHandleBuffer = stack.mallocPointer(1);
      int err = vkCreateDevice(physicalDevice, createInfo, null, deviceHandleBuffer);
      if (err != VK_SUCCESS) {
        throw new AssertionError("Failed to create logical device: " + err);
      }
      return new VkDevice(deviceHandleBuffer.get(), physicalDevice, createInfo);
    }
  }

  private static VkQueue createGraphicsQueue(VkDevice device, QueueFamilyIndices indices) {
    try (MemoryStack stack = stackPush()) {
      PointerBuffer queueHandleBuffer = stack.mallocPointer(1);
      vkGetDeviceQueue(device, indices.getGraphicsFamilyIndex(), 0, queueHandleBuffer);
      long queueHandle = queueHandleBuffer.get(0);
      return new VkQueue(queueHandle, device);
    }
  }

  private static VkQueue createPresentQueue(VkDevice device, QueueFamilyIndices indices) {
    try (MemoryStack stack = stackPush()) {
      PointerBuffer queueHandleBuffer = stack.mallocPointer(1);
      vkGetDeviceQueue(device, indices.getPresentFamilyIndex(), 0, queueHandleBuffer);
      long queueHandle = queueHandleBuffer.get(0);
      return new VkQueue(queueHandle, device);
    }
  }

  private static boolean isDeviceSuitable(VkPhysicalDevice device, long surface) {
    QueueFamilyIndices indices = findQueueFamilies(device, surface);

    return indices.isComplete();
  }

  private static QueueFamilyIndices findQueueFamilies(VkPhysicalDevice device, long surface) {
    try (MemoryStack stack = stackPush()) {
      IntBuffer queueFamilyCountBuffer = stack.mallocInt(1);
      vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCountBuffer, null);
      int queueFamilyCount = queueFamilyCountBuffer.get(0);

      VkQueueFamilyProperties.Buffer queueFamilyPropertiesBuffer =
          VkQueueFamilyProperties.callocStack(queueFamilyCount);
      vkGetPhysicalDeviceQueueFamilyProperties(
          device, queueFamilyCountBuffer, queueFamilyPropertiesBuffer);

      QueueFamilyIndices indices = new QueueFamilyIndices();
      IntBuffer presentSupportBuffer = stack.mallocInt(1);
      for (int i = 0; i < queueFamilyCount; ++i) {
        VkQueueFamilyProperties queueFamily = queueFamilyPropertiesBuffer.get(i);
        if (queueFamily.queueCount() > 0) {
          if ((queueFamily.queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0) {
            indices.setGraphicsFamilyIndex(i);
          }

          presentSupportBuffer.rewind();
          vkGetPhysicalDeviceSurfaceSupportKHR(device, i, surface, presentSupportBuffer);
          if (presentSupportBuffer.get(0) == VK_TRUE) {
            indices.setPresentFamilyIndex(i);
          }

          if (indices.isComplete()) {
            break;
          }
        }
      }
      return indices;
    }
  }

  private static PointerBuffer getRequiredExtensions() {
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

  private static boolean checkValidationLayerSupport() {
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
    new WindowSurface().run();
  }
}
