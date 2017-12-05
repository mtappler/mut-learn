package at.tugraz.mutation_equiv;

import java.util.ArrayList;
import java.util.List;

import de.learnlib.api.SUL;
import de.learnlib.api.SULException;
import net.automatalib.words.impl.Symbol;

public class TestTrackingSUL implements SUL<Symbol, String> {
	private SUL<Symbol, String> actualSUL;
	private List<List<Symbol>> executedTests = new ArrayList<>();
	private List<Symbol> currentTest = null;
	private boolean track = true;

	public TestTrackingSUL(SUL<Symbol, String> actualSUL) {
		this.actualSUL = actualSUL;
	}

	@Override
	public void pre() {
		if (track)
			currentTest = new ArrayList<>();
		actualSUL.pre();
	}

	@Override
	public void post() {
		if (track)
			getExecutedTests().add(currentTest);
		actualSUL.post();
	}

	@Override
	public String step(Symbol in) throws SULException {
		if (track)
			currentTest.add(in);
		return actualSUL.step(in);
	}

	public boolean isTrack() {
		return track;
	}

	public void setTrack(boolean track) {
		this.track = track;
	}

	public void clear() {
		getExecutedTests().clear();
	}

	public List<List<Symbol>> getExecutedTests() {
		return executedTests;
	}
}
