import { json } from '@sveltejs/kit';
import type { RequestHandler } from './$types';
import { OPENAI_API_KEY } from '$env/static/private';

export const POST: RequestHandler = async ({ request }) => {
    const formData = await request.formData();
    const audioFile = formData.get('file') as Blob;

    if (!audioFile) {
        return json({ error: 'No audio file provided' }, { status: 400 });
    }

    const apiKey = OPENAI_API_KEY;

    if (!apiKey || apiKey.startsWith('sk-placeholder')) {
        // [Mock] Fallback if no key is configured, to avoid breaking the UX demo
        return json({ text: "Simulated transcription: OpenAI API Key is missing or invalid in .env." });
    }

    const openaiFormData = new FormData();
    openaiFormData.append('file', audioFile, 'speech.webm');
    openaiFormData.append('model', 'whisper-1');

    try {
        const response = await fetch('https://api.openai.com/v1/audio/transcriptions', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${apiKey}`
            },
            body: openaiFormData
        });

        if (!response.ok) {
            const err = await response.json();
            return json({ error: 'OpenAI API Error', details: err }, { status: response.status });
        }

        const data = await response.json();
        return json({ text: data.text });
    } catch (e) {
        console.error('Transcription failed:', e);
        return json({ error: 'Internal Server Error' }, { status: 500 });
    }
};
