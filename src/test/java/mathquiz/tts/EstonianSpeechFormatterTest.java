package mathquiz.tts;

import mathquiz.domain.Operation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EstonianSpeechFormatterTest {

    @Test
    void numberToEstonian_singleDigits() {
        assertThat(EstonianSpeechFormatter.numberToEstonian(0)).isEqualTo("null");
        assertThat(EstonianSpeechFormatter.numberToEstonian(1)).isEqualTo("üks");
        assertThat(EstonianSpeechFormatter.numberToEstonian(5)).isEqualTo("viis");
        assertThat(EstonianSpeechFormatter.numberToEstonian(9)).isEqualTo("üheksa");
    }

    @Test
    void numberToEstonian_teens() {
        assertThat(EstonianSpeechFormatter.numberToEstonian(10)).isEqualTo("kümme");
        assertThat(EstonianSpeechFormatter.numberToEstonian(11)).isEqualTo("üksteist");
        assertThat(EstonianSpeechFormatter.numberToEstonian(15)).isEqualTo("viisteist");
        assertThat(EstonianSpeechFormatter.numberToEstonian(19)).isEqualTo("üheksateist");
    }

    @Test
    void numberToEstonian_tens() {
        assertThat(EstonianSpeechFormatter.numberToEstonian(20)).isEqualTo("kakskümmend");
        assertThat(EstonianSpeechFormatter.numberToEstonian(30)).isEqualTo("kolmkümmend");
        assertThat(EstonianSpeechFormatter.numberToEstonian(50)).isEqualTo("viiskümmend");
    }

    @Test
    void numberToEstonian_compound() {
        assertThat(EstonianSpeechFormatter.numberToEstonian(21)).isEqualTo("kakskümmend üks");
        assertThat(EstonianSpeechFormatter.numberToEstonian(35)).isEqualTo("kolmkümmend viis");
        assertThat(EstonianSpeechFormatter.numberToEstonian(99)).isEqualTo("üheksakümmend üheksa");
    }

    @Test
    void numberToEstonian_negative() {
        assertThat(EstonianSpeechFormatter.numberToEstonian(-5)).isEqualTo("miinus viis");
    }

    @Test
    void formatProblem_addition() {
        String result = EstonianSpeechFormatter.formatProblem(2, Operation.ADDITION, 3);
        assertThat(result).isEqualTo("Kui palju on kaks pluss kolm?");
    }

    @Test
    void formatProblem_subtraction() {
        String result = EstonianSpeechFormatter.formatProblem(10, Operation.SUBTRACTION, 4);
        assertThat(result).isEqualTo("Kui palju on kümme miinus neli?");
    }

    @Test
    void formatProblem_multiplication() {
        String result = EstonianSpeechFormatter.formatProblem(5, Operation.MULTIPLICATION, 5);
        assertThat(result).isEqualTo("Kui palju on viis korda viis?");
    }

    @Test
    void formatProblem_division() {
        String result = EstonianSpeechFormatter.formatProblem(20, Operation.DIVISION, 4);
        assertThat(result).isEqualTo("Kui palju on kakskümmend jagatud neli?");
    }

    @Test
    void formatCorrect() {
        String result = EstonianSpeechFormatter.formatCorrect(2, Operation.ADDITION, 3, 5);
        assertThat(result).isEqualTo("Tubli, kaks pluss kolm on viis.");
    }

    @Test
    void formatIncorrect() {
        String result = EstonianSpeechFormatter.formatIncorrect(2, Operation.ADDITION, 3, 5);
        assertThat(result).isEqualTo("Kahjuks vale, kaks pluss kolm on viis.");
    }
}
