@import mathquiz.domain.Operation

@param int operand1
@param int operand2
@param Operation operation

<div class="problem ${operation.name().toLowerCase()}">
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
</div>
