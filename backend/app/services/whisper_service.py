import whisper

from app.config import settings


class WhisperService:
    """Service for speech-to-text transcription using OpenAI Whisper.
    
    Attributes:
        model: The loaded Whisper model instance.
        model_name: Name of the Whisper model to use (e.g., 'small', 'medium', 'large').
        language: Target language for transcription (e.g., 'id' for Indonesian).
    """

    def __init__(self, model_name: str = "small", language: str = "id"):
        self.model = None
        self.model_name = model_name
        self.language = language

    def load_model(self):
        """Load Whisper model into memory.
        
        Should be called on application startup for faster first transcription,
        or will be called automatically on first transcribe() call.
        """
        self.model = whisper.load_model(self.model_name, download_root="E:\\Untuk Library\\whisper")

    def transcribe(self, audio_path: str) -> str:
        """Transcribe an audio file to text.
        
        Args:
            audio_path: Path to the audio file to transcribe.
        
        Returns:
            Transcribed text string.
        """
        if self.model is None:
            self.load_model()
        
        # initial_prompt with punctuation forces Whisper to generate punctuation in the output.
        # This is CRITICAL for TextRank because without punctuation, the entire text 
        # is treated as a single sentence and won't be summarized.
        prompt = "Halo, selamat pagi. Berikut adalah transkripsi rapat hari ini, lengkap dengan tanda baca. Apakah suara saya terdengar jelas? Baik, mari kita mulai diskusinya!"
        
        result = self.model.transcribe(
            audio_path, 
            language=self.language,
            initial_prompt=prompt
        )
        return result["text"]


# Singleton instance using settings
whisper_service = WhisperService(
    model_name=settings.WHISPER_MODEL,
    language=settings.WHISPER_LANGUAGE,
)
