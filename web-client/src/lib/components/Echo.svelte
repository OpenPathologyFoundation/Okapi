<script lang="ts">
    import { onMount, tick } from "svelte";
    import { fade, fly } from "svelte/transition";

    let isOpen = false;
    let isListening = false;
    let isSuccess = false; // New success state
    let transcript = "";

    // Audio State
    let mediaRecorder: MediaRecorder | null = null;
    let audioChunks: Blob[] = [];
    let audioContext: AudioContext | null = null;
    let analyser: AnalyserNode | null = null;
    let microphoneStream: MediaStream | null = null;
    let animationFrameId: number;
    let audioLevels: number[] = new Array(5).fill(10); // 5 bars

    // PTT State
    let spacePressedTime = 0;

    // [Mock] Context Capture
    const contextData = {
        caseId: "CASE-2026-X99",
        zoom: "40x",
        activeTool: "Heatmap v2",
        latency: "42ms",
    };

    function toggleEcho() {
        if (isSuccess) {
            // Reset if opening after success
            isSuccess = false;
            transcript = "";
        }
        isOpen = !isOpen;
        if (!isOpen) {
            stopListening(); // Safety close
        }
    }

    function handleKeydown(e: KeyboardEvent) {
        if (e.key === "~" || e.key === "F1") {
            e.preventDefault();
            toggleEcho();
        }
    }

    // --- Audio Visualization Logic ---
    function updateVisualizer() {
        if (!analyser || !isListening) return;
        const dataArray = new Uint8Array(analyser.frequencyBinCount);
        analyser.getByteFrequencyData(dataArray);

        // Calculate average volume for a few bands
        // Only 5 bars, so let's sample 5 roughly equal chunks
        const step = Math.floor(dataArray.length / 5);
        const levels = [];

        for (let i = 0; i < 5; i++) {
            let sum = 0;
            for (let j = 0; j < step; j++) sum += dataArray[i * step + j];
            const avg = sum / step;
            const height = Math.max(4, (avg / 255) * 48);
            levels.push(height);
        }
        audioLevels = levels;
        animationFrameId = requestAnimationFrame(updateVisualizer);
    }

    // --- Recording Logic ---
    async function startRecording() {
        if (isListening) return;
        try {
            const stream = await navigator.mediaDevices.getUserMedia({
                audio: true,
            });
            microphoneStream = stream;
            audioContext = new AudioContext();
            analyser = audioContext.createAnalyser();
            analyser.fftSize = 64;
            const source = audioContext.createMediaStreamSource(stream);
            source.connect(analyser);
            mediaRecorder = new MediaRecorder(stream, {
                mimeType: "audio/webm",
            });
            audioChunks = [];

            mediaRecorder.ondataavailable = (event) => {
                if (event.data.size > 0) audioChunks.push(event.data);
            };

            mediaRecorder.onstop = async () => {
                const audioBlob = new Blob(audioChunks, { type: "audio/webm" });
                await processAudio(audioBlob);
                if (microphoneStream)
                    microphoneStream
                        .getTracks()
                        .forEach((track) => track.stop());
                if (audioContext && audioContext.state !== "closed")
                    audioContext.close();
                cancelAnimationFrame(animationFrameId);
                audioLevels = new Array(5).fill(4);
            };

            mediaRecorder.start();
            isListening = true;
            updateVisualizer();
        } catch (err) {
            console.error("Mic access denied:", err);
            transcript = "Error: Microphone access denied.";
        }
    }

    function stopListening() {
        if (!isListening || !mediaRecorder) return;
        mediaRecorder.stop();
        isListening = false;
    }

    async function processAudio(blob: Blob) {
        if (blob.size < 100) return;
        const formData = new FormData();
        formData.append("file", blob);
        try {
            const res = await fetch("/api/echo/transcribe", {
                method: "POST",
                body: formData,
            });
            if (!res.ok) throw new Error(res.statusText);
            const data = await res.json();
            if (data.text) {
                transcript = (transcript ? transcript + " " : "") + data.text;
            } else if (data.error) {
                transcript =
                    (transcript ? transcript + " " : "") +
                    "[Error: " +
                    (data.details?.message || "Transcribe failed") +
                    "]";
            }
        } catch (e) {
            transcript =
                (transcript ? transcript + " " : "") + "[Upload Failed]";
        }
    }

    // --- PTT Logic ---
    function handleWindowKeydown(e: KeyboardEvent) {
        if (!isOpen) {
            if ((e.key === "~" || e.key === "F1") && !e.repeat) {
                e.preventDefault();
                toggleEcho();
            }
            return;
        }

        if (e.code === "Space" && !e.repeat) {
            // Only block space if input is focused
            if (
                document.activeElement?.tagName === "TEXTAREA" ||
                document.activeElement?.tagName === "INPUT"
            ) {
                e.preventDefault();
            }
            spacePressedTime = Date.now();
            startRecording();
        }
    }

    function handleWindowKeyup(e: KeyboardEvent) {
        if (!isOpen) return;
        if (e.code === "Space") {
            if (isListening) stopListening();
        }
    }

    function handleSubmit() {
        if (!transcript.trim()) return;

        console.log("[Echo] Feedback Logged:", {
            ...contextData,
            userFeedback: transcript,
            timestamp: new Date().toISOString(),
        });

        // Success UX
        isSuccess = true;
        setTimeout(() => {
            isOpen = false;
            // Delay reset slightly so it fades out with success message
            setTimeout(() => {
                isSuccess = false;
                transcript = "";
            }, 300);
        }, 1500); // Show "Thank you" for 1.5s
    }

    function handleInputKeydown(e: KeyboardEvent) {
        if (e.key === "Enter" && !e.shiftKey) {
            // Allow Shift+Enter for new lines
            e.preventDefault();
            handleSubmit();
        }
    }
</script>

<svelte:window on:keydown={handleWindowKeydown} on:keyup={handleWindowKeyup} />

<!-- [A] THE ANCHOR (Sidebar Integrated) -->
<button
    on:click={(e) => {
        e.stopPropagation();
        toggleEcho();
    }}
    class="relative h-10 w-10 rounded-full bg-clinical-surface border border-clinical-border flex items-center justify-center group transition-all duration-300 hover:bg-amber-500/10 hover:border-amber-500/60 {isListening
        ? 'animate-pulse ring-2 ring-amber-500'
        : ''}"
    title="Ask questions or provide feedback (~)"
>
    <!-- Icon: Mic + Cursor -->
    <svg
        class="w-5 h-5 text-clinical-muted group-hover:text-amber-400 transition-colors"
        fill="none"
        viewBox="0 0 24 24"
        stroke="currentColor"
    >
        <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="1.5"
            d="M19 11a7 7 0 01-7 7m0 0a7 7 0 01-7-7m7 7v4m0 0H8m4 0h4m-4-8a3 3 0 01-3-3V5a3 3 0 116 0v6a3 3 0 01-3 3z"
        />
    </svg>

    <!-- Pulse dot for attention (idle state) -->
    <span
        class="absolute top-0 right-0 h-2 w-2 rounded-full bg-amber-500/0 group-hover:bg-amber-500 transition-all shadow-[0_0_8px_rgba(245,158,11,0.6)]"
    ></span>
</button>

<!-- [B] THE CONTEXT CAPSULE (Modal) -->
{#if isOpen}
    <!-- svelte-ignore a11y_click_events_have_key_events -->
    <!-- svelte-ignore a11y_no_static_element_interactions -->
    <div
        transition:fade={{ duration: 200 }}
        class="fixed inset-0 bg-black/60 backdrop-blur-sm z-[100]"
        on:click={toggleEcho}
    ></div>

    <!-- svelte-ignore a11y_click_events_have_key_events -->
    <!-- svelte-ignore a11y_no_static_element_interactions -->
    <div
        transition:fly={{ y: 20, duration: 300 }}
        class="fixed bottom-20 left-20 z-[101] w-[480px] bg-gray-900/95 backdrop-blur-xl border border-gray-700/50 rounded-2xl shadow-2xl overflow-hidden ring-1 ring-white/10"
        on:click={(e) => e.stopPropagation()}
    >
        <!-- Header -->
        <div
            class="px-5 py-3 bg-white/5 border-b border-white/5 flex items-center justify-between text-[10px] uppercase tracking-wider text-gray-400 font-mono"
        >
            <span>Ctx: {contextData.caseId}</span>
            <span>Zm: {contextData.zoom}</span>
            <span class="text-amber-500">Echo Active</span>
        </div>

        {#if isSuccess}
            <!-- Success State -->
            <div
                class="p-8 flex flex-col items-center justify-center text-center animate-in fade-in zoom-in duration-300"
            >
                <div
                    class="h-12 w-12 rounded-full bg-green-500/20 flex items-center justify-center mb-3"
                >
                    <svg
                        class="w-6 h-6 text-green-400"
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                    >
                        <path
                            stroke-linecap="round"
                            stroke-linejoin="round"
                            stroke-width="2"
                            d="M5 13l4 4L19 7"
                        />
                    </svg>
                </div>
                <h3 class="text-white text-lg font-medium">Captured.</h3>
                <p class="text-gray-400 text-sm mt-1">
                    Thank you for your feedback.
                </p>
            </div>
        {:else}
            <!-- Standard Input State -->
            <div class="p-6">
                <!-- Hint Line -->
                <div
                    class="text-gray-500 text-sm font-light italic mb-2 min-h-[1.25rem] flex items-center gap-2"
                >
                    {#if isListening}
                        <span
                            class="inline-block w-2 h-2 rounded-full bg-amber-500 animate-pulse"
                        ></span>
                        <span class="text-amber-500">Listening...</span>
                    {:else}
                        Type or hold Space to speak...
                    {/if}
                </div>

                <!-- Input Area (Expanded) -->
                <div class="relative flex gap-3">
                    <!-- Mic Trigger -->
                    <button
                        on:mousedown={startRecording}
                        on:mouseup={stopListening}
                        on:mouseleave={stopListening}
                        class="mt-1 h-8 w-8 rounded-full bg-white/5 hover:bg-amber-500/20 flex items-center justify-center transition-all shrink-0 {isListening
                            ? 'text-amber-500 ring-1 ring-amber-500'
                            : 'text-gray-500'}"
                        title="Click and Hold to Speak"
                    >
                        <svg
                            class="w-4 h-4"
                            fill="none"
                            viewBox="0 0 24 24"
                            stroke="currentColor"
                        >
                            <path
                                stroke-linecap="round"
                                stroke-linejoin="round"
                                stroke-width="2"
                                d="M19 11a7 7 0 01-7 7m0 0a7 7 0 01-7-7m7 7v4m0 0H8m4 0h4m-4-8a3 3 0 01-3-3V5a3 3 0 116 0v6a3 3 0 01-3 3z"
                            />
                        </svg>
                    </button>

                    {#if isListening}
                        <!-- Live Visualizer -->
                        <div class="flex items-center gap-1 flex-1 h-[60px]">
                            <!-- Taller for visualizer -->
                            {#each audioLevels as height}
                                <div
                                    class="w-2 bg-amber-500 rounded-full transition-all duration-75 ease-out"
                                    style="height: {height *
                                        1.5}px; opacity: {0.5 +
                                        (height / 48) * 0.5}"
                                ></div>
                            {/each}
                        </div>
                    {:else}
                        <!-- Text Area for Multi-line -->
                        <!-- svelte-ignore a11y_autofocus -->
                        <textarea
                            bind:value={transcript}
                            on:keydown={handleInputKeydown}
                            class="w-full h-[80px] bg-transparent border-none text-gray-200 focus:ring-0 p-0 text-sm font-mono placeholder:text-gray-600 resize-none focus:outline-none leading-relaxed"
                            placeholder=">_ Describe the issue or request..."
                            autofocus
                        ></textarea>
                    {/if}
                </div>
            </div>

            <!-- Footer -->
            <div
                class="px-5 py-2 bg-black/20 text-[10px] text-gray-600 flex justify-between"
            >
                <span>[Space] HOLD to Speak</span>
                <span>[Enter] Submit</span>
            </div>
        {/if}
    </div>
{/if}
