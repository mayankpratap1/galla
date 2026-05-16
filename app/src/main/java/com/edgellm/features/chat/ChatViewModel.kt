package com.edgellm.features.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edgellm.skills.SkillManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val modelLoaded: Boolean = false,
    val error: String? = null
)

class ChatViewModel : ViewModel() {
    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state

    var engineRef: com.edgellm.engine.InferenceEngine? = null
    var skillManager: SkillManager? = null
    var agentSkillsEnabled: Boolean = false

    // Called from MainNavigation after service binds
    fun setModelLoaded(loaded: Boolean) {
        _state.value = _state.value.copy(modelLoaded = loaded)
    }

    fun sendMessage(text: String) {
        val engine = engineRef ?: run {
            _state.value = _state.value.copy(error = "No model loaded. Go to Settings to load a model.")
            return
        }

        val history = _state.value.messages + ChatMessage("user", text)
        _state.value = _state.value.copy(
            messages = history + ChatMessage("assistant", ""),
            isGenerating = true,
            error = null
        )

        viewModelScope.launch {
            val systemPrompt = if (agentSkillsEnabled) {
                skillManager?.buildSkillSystemPrompt(skillManager!!.skills.value) ?: ""
            } else ""

            val promptParts = buildList {
                if (systemPrompt.isNotEmpty()) add("System: $systemPrompt")
                history.forEach { add("${it.role.replaceFirstChar { c -> c.uppercase() }}: ${it.content}") }
                add("Assistant:")
            }
            val prompt = promptParts.joinToString("\n")

            var fullText = ""
            try {
                engine.generateStream(prompt).collect { token ->
                    fullText += token
                    val (thinking, display) = processThinkingTags(fullText)
                    val updated = _state.value.messages.dropLast(1) +
                        ChatMessage("assistant", display, thinking.ifEmpty { null })
                    _state.value = _state.value.copy(messages = updated)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Generation error: ${e.message}")
            } finally {
                _state.value = _state.value.copy(isGenerating = false)
            }
        }
    }

    // Extracts <think>...</think> → Pair(thinkingText, displayText)
    private fun processThinkingTags(text: String): Pair<String, String> {
        val thinkRegex = Regex("<think>(.*?)</think>", RegexOption.DOT_MATCHES_ALL)
        val thinking = thinkRegex.findAll(text).joinToString("\n") { it.groupValues[1] }
        val display  = thinkRegex.replace(text, "").trim()
        return Pair(thinking, display)
    }
}
