import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class DummyLogSource : LogSource {
    override fun lines() = flowOf(
        "D/EGL_emulation( 2788): app_time_stats: avg=20003.83ms min=20003.83ms max=20003.83ms count=1",
        "D/BoundBrokerSvc(12048): onUnbind: Intent { act=com.google.android.gms.auth.key.retrieval.service.START dat=chimera-action: cmp=com.google.android.gms/.chimera.GmsApiService }",
        "D/EGL_emulation( 2788): app_time_stats: avg=3860.65ms min=3860.65ms max=3860.65ms count=1",
        "I/ChimeraPrvdrProxy(12048): Shutting down chimera ContentProvider com.google.android.gms.reminders.provider.RemindersChimeraProvider",
        "D/ConnectivityService( 2573): requestNetwork for uid/pid:10115/15022 activeRequest: null callbackRequest: 1713 [NetworkRequest [ REQUEST id=1714, [ Capabilities: INTERNET&NOT_RESTRICTED&TRUSTED&NOT_VCN_MANAGED Uid: 10115 RequestorUid: 10115 RequestorPkg: com.android.chrome] ]] callback flags: 0 priority: 2147483647",
        "D/ConnectivityService( 2573): NetReassign [1714 : null → 105]",
        "D/WifiNetworkFactory( 2573): got request NetworkRequest [ REQUEST id=1714, [ Capabilities: INTERNET&NOT_RESTRICTED&TRUSTED&NOT_VCN_MANAGED Uid: 10115 RequestorUid: 10115 RequestorPkg: com.android.chrome] ]",
        "D/UntrustedWifiNetworkFactory( 2573): got request NetworkRequest [ REQUEST id=1714, [ Capabilities: INTERNET&NOT_RESTRICTED&TRUSTED&NOT_VCN_MANAGED Uid: 10115 RequestorUid: 10115 RequestorPkg: com.android.chrome] ]",
        "D/OemPaidWifiNetworkFactory( 2573): got request NetworkRequest [ REQUEST id=1714, [ Capabilities: INTERNET&NOT_RESTRICTED&TRUSTED&NOT_VCN_MANAGED Uid: 10115 RequestorUid: 10115 RequestorPkg: com.android.chrome] ]",
        "D/ConnectivityService( 2573): NetReassign [no changes]",
        "E/cr_BTSPrefs(15022): No data found for task id: 53",
        "E/cr_BkgrdTaskScheduler(15022): Task cannot be canceled because no data was found instorage or data was invalid",
        "W/Looper  ( 2573): Slow delivery took 4921ms main h=android.os.Handler c=com.android.internal.os.BinderCallsStats\$1@a16bc63 m=0",
        "V/GraphicsEnvironment(15121): ANGLE Developer option for 'com.android.chrome' set to: 'default'",
        "V/GraphicsEnvironment(15121): Neither updatable production driver nor prerelease driver is supported."

/*
        "D/CompatibilityChangeReporter(15109): Compat change id reported: 171979766; UID 90000; state: DISABLED\n" +
                "W/ileged_process(15121): Unexpected CPU variant for X86 using defaults: x86_64\n" +
                "D/NetworkSecurityConfig(15109): Using Network Security Config from resource 0_resource_name_obfuscated debugBuild: false\n" +
                "D/NetworkSecurityConfig(15109): Using Network Security Config from resource 0_resource_name_obfuscated debugBuild: false\n" +
                "W/SystemServiceRegistry(15109): No service published for: uimode\n" +
                "E/ileged_process(15121): Not starting debugger since process cannot load the jdwp agent.\n" +
                "I/cr_ChildProcessService(15109): Creating new ChildProcessService pid=15109\n" +
                "D/CompatibilityChangeReporter(15121): Compat change id reported: 171979766; UID 10115; state: DISABLED\n" +
                "I/ActivityManager( 2573): Killing 14142:com.google.process.gapps/u0a96 (adj 975): empty #17\n" +
                "I/cr_LibraryLoader(15109): Loaded native library version number \"91.0.4472.114\"\n" +
                "I/cr_CachingUmaRecorder(15109): Flushed 1 samples from 1 histograms.\n" +
                "I/Zygote  ( 2502): Process 14142 exited due to signal 9 (Killed)\n" +
                "V/GraphicsEnvironment(15121): ANGLE Developer option for 'com.android.chrome' set to: 'default'\n" +
                "V/GraphicsEnvironment(15121): Neither updatable production driver nor prerelease driver is supported.\n" +
                "D/NetworkSecurityConfig(15121): Using Network Security Config from resource 0_resource_name_obfuscated debugBuild: false\n" +
                "I/libprocessgroup( 2573): Successfully killed process cgroup uid 10096 pid 14142 in 47ms\n" +
                "D/NetworkSecurityConfig(15121): Using Network Security Config from resource 0_resource_name_obfuscated debugBuild: false\n" +
                "I/cr_ChildProcessService(15121): Creating new ChildProcessService pid=15121\n" +
                "I/cr_LibraryLoader(15121): Using linker: org.chromium.base.library_loader.ModernLinker\n" +
                "E/cr_ChromiumAndroidLinker(15121): ReserveAddressWithHint: Address range starting at 0x73c6826f1000 was not free to use\n" +
                "I/cr_LibraryLoader(15121): Loading monochrome_64\n" +
                "I/cr_ModernLinker(15121): loadLibraryImplLocked: monochrome_64, relroMode=2\n" +
                "E/cr_ChromiumAndroidLinker(15121): LoadLibrary: Failed to load native library: libmonochrome_64.so\n" +
                "E/cr_ModernLinker(15121): Unable to load library: libmonochrome_64.so\n" +
                "W/cr_Linker(15121): Failed to load native library with shared RELRO, retrying without\n" +
                "I/cr_ModernLinker(15121): loadLibraryImplLocked: monochrome_64, relroMode=0\n" +
                "I/ActivityManager( 2573): Killing 14190:com.google.android.webview:webview_service/u0a117 (adj 975): empty #17\n" +
                "I/ActivityManager( 2573): Killing 14433:com.google.android.apps.maps/u0a114 (adj 965): empty #17\n" +
                "I/ActivityManager( 2573): Killing 14395:com.android.statementservice/u0a70 (adj 965): empty #18\n" +
                "I/ActivityManager( 2573): Killing 14259:android.process.acore/u0a59 (adj 975): empty #19\n" +
                "I/Zygote  ( 2502): Process 14190 exited due to signal 9 (Killed)\n" +
                "I/Zygote  ( 2502): Process 14395 exited due to signal 9 (Killed)\n" +
                "D/ConnectivityService( 2573): ConnectivityService NetworkRequestInfo binderDied(uid/pid:10114/14433, android.os.BinderProxy@288444c)\n" +
                "D/ConnectivityService( 2573): ConnectivityService NetworkRequestInfo binderDied(uid/pid:10114/14433, android.os.BinderProxy@79c6c95)\n" +
                "D/ConnectivityService( 2573): ConnectivityService NetworkRequestInfo binderDied(uid/pid:10114/14433, android.os.BinderProxy@6edd1aa)\n" +
                "D/ConnectivityService( 2573): releasing NetworkRequest [ REQUEST id=1681, [ Capabilities: INTERNET&NOT_RESTRICTED&TRUSTED&NOT_VCN_MANAGED Uid: 10114 RequestorUid: 10114 RequestorPkg: com.google.android.apps.maps] ] (release request)\n" +
                "D/ConnectivityService( 2573): releasing NetworkRequest [ REQUEST id=1683, [ Capabilities: INTERNET&NOT_RESTRICTED&TRUSTED&NOT_VCN_MANAGED Uid: 10114 RequestorUid: 10114 RequestorPkg: com.google.android.apps.maps] ] (release request)\n" +
                "I/Zygote  ( 2502): Process 14433 exited due to signal 9 (Killed)\n" +
                "D/CompatibilityChangeReporter( 2573): Compat change id reported: 171306433; UID 10114; state: ENABLED\n" +
                "I/libprocessgroup( 2573): Successfully killed process cgroup uid 10117 pid 14190 in 45ms\n" +
                "E/cr_ChromiumAndroidLinker(15121): FindRelroAndLibraryRangesInElf: Failed to find library at address 0x73c6826f1000\n" +
                "E/cr_ChromiumAndroidLinker(15121): CompareRelroAndReplaceItBy: Could not find RELRO from externally provided address: 0x0x73c6826f1000\n" +
                "I/libprocessgroup( 2573): Successfully killed process cgroup uid 10114 pid 14433 in 0ms\n" +
                "I/libprocessgroup( 2573): Successfully killed process cgroup uid 10070 pid 14395 in 0ms\n" +
                "I/libprocessgroup( 2573): Successfully killed process cgroup uid 10059 pid 14259 in 0ms\n" +
                "I/cr_LibraryLoader(15121): Loaded native library version number \"91.0.4472.114\"\n" +
                "I/cr_CachingUmaRecorder(15121): Flushed 3 samples from 3 histograms.\n" +
                "D/CountryDetector( 2573): No listener is left\n" +
                "I/Zygote  ( 2502): Process 14259 exited due to signal 9 (Killed)\n" +
                "D/libEGL  (15121): loaded /vendor/lib64/egl/libEGL_emulation.so\n" +
                "D/libEGL  (15121): loaded /vendor/lib64/egl/libGLESv1_CM_emulation.so\n" +
                "D/libEGL  (15121): loaded /vendor/lib64/egl/libGLESv2_emulation.so\n" +
                "D/HostConnection(15121): createUnique: call\n" +
                "D/HostConnection(15121): HostConnection::get() New Host Connection established 0x73c7988e09d0, tid 15146\n" +
                "D/HostConnection(15121): HostComposition ext ANDROID_EMU_CHECKSUM_HELPER_v1 ANDROID_EMU_native_sync_v2 ANDROID_EMU_native_sync_v3 ANDROID_EMU_native_sync_v4 ANDROID_EMU_dma_v1 ANDROID_EMU_direct_mem ANDROID_EMU_YUV_Cache ANDROID_EMU_has_shared_slots_host_memory_allocator ANDROID_EMU_sync_buffer_data ANDROID_EMU_read_color_buffer_dma GL_OES_EGL_image_external_essl3 GL_OES_vertex_array_object GL_KHR_texture_compression_astc_ldr ANDROID_EMU_host_side_tracing ANDROID_EMU_gles_max_version_3_0\n" +
                "I/hwservicemanager(  194): getTransport: Cannot find entry android.hardware.configstore@1.0::ISurfaceFlingerConfigs/default in either framework or device VINTF manifest.\n" +
                "D/EGL_emulation(15121): eglCreateContext: 0x73c7988dfa10: maj 3 min 0 rcv 3\n" +
                "D/EGL_emulation(15121): eglMakeCurrent: 0x73c7988dfa10: ver 3 0 (tinfo 0x73c9b02ad080) (first time)\n" +
                "D/vulkan  (15121): searching for layers in '/data/app/~~TlwRnm7DunyD5AlJ1EBTuQ==/com.android.chrome-acEL0mTKOJUIH6bLx_hRQw==/lib/x86_64'\n" +
                "D/vulkan  (15121): searching for layers in '/data/app/~~TlwRnm7DunyD5AlJ1EBTuQ==/com.android.chrome-acEL0mTKOJUIH6bLx_hRQw==/Chrome.apk!/lib/x86_64'\n" +
                "D/vulkan  (15121): searching for layers in '/data/app/~~Oc6wVdam0gpWvukrP5N98Q==/com.google.android.trichromelibrary_447211487-U_r2YzGjWLx5HIe6JzKL5Q==/TrichromeLibrary.apk!/lib/x86_64'\n" +
                "D/EGL_emulation(15121): eglCreateContext: 0x73c7988dfa10: maj 3 min 0 rcv 3\n" +
                "W/YouTubeMusic(14520): Could not register for notifications with InnerTube:\n" +
                "W/YouTubeMusic(14520): xuz: dll: java.lang.IllegalStateException: DefaultAccountIdResolver could not resolve pseudonymous, pseudonymous\n" +
                "W/YouTubeMusic(14520): \tat xup.apply(Unknown Source:4)\n" +
                "W/YouTubeMusic(14520): \tat vwt.p(PG:5)\n" +
                "W/YouTubeMusic(14520): \tat vwt.b(PG:2)\n" +
                "W/YouTubeMusic(14520): \tat xur.d(PG:4)\n" +
                "W/YouTubeMusic(14520): \tat acvb.c(PG:51)\n" +
                "W/YouTubeMusic(14520): \tat acvb.j(PG:38)\n" +
                "W/YouTubeMusic(14520): \tat acvb.d(PG:3)\n" +
                "W/YouTubeMusic(14520): \tat acuz.run(Unknown Source:2)\n" +
                "W/YouTubeMusic(14520): \tat java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:462)\n" +
                "W/YouTubeMusic(14520): \tat java.util.concurrent.FutureTask.run(FutureTask.java:266)\n" +
                "W/YouTubeMusic(14520): \tat java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)\n" +
                "W/YouTubeMusic(14520): \tat java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)\n" +
                "W/YouTubeMusic(14520): \tat qht.run(PG:2)\n" +
                "W/YouTubeMusic(14520): \tat qif.run(PG:4)\n" +
                "W/YouTubeMusic(14520): \tat java.lang.Thread.run(Thread.java:920)\n" +
                "W/YouTubeMusic(14520): Caused by: dll: java.lang.IllegalStateException: DefaultAccountIdResolver could not resolve pseudonymous, pseudonymous\n" +
                "W/YouTubeMusic(14520): \tat wez.b(PG:1)\n" +
                "W/YouTubeMusic(14520): \tat wez.run(PG:24)\n" +
                "W/YouTubeMusic(14520): \tat java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)\n" +
                "W/YouTubeMusic(14520): \tat java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)\n" +
                "W/YouTubeMusic(14520): \tat vwa.run(PG:11)\n" +
                "W/YouTubeMusic(14520): \t... 1 more",
*/

    )
}