@import mathquiz.domain.Operation

@param int operand1
@param int operand2
@param Operation operation
@param String audioHash = null

<div class="problem ${operation.name().toLowerCase()}" data-audio-hash="${audioHash}">
    <div class="equation">
        <span class="operand">${operand1}</span>
        <span class="operator">${operation.symbol()}</span>
        <span class="operand">${operand2}</span>
        <span class="equals">=</span>
        <span class="answer-box">?</span>
    </div>

    <form hx-post="/quiz/answer"
          hx-target="#problem-area"
          hx-swap="innerHTML"
          class="answer-form">
        <input type="number"
               name="answer"
               class="answer-input"
               autocomplete="off"
               inputmode="numeric"
               autofocus
               required>
        <button type="submit" class="submit-button">Vasta</button>
    </form>

    <div class="audio-hint">Vajuta R heli kordamiseks</div>
</div>
