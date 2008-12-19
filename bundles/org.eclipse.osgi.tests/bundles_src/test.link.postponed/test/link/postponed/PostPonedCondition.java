package test.link.postponed;

import java.util.Dictionary;
import org.osgi.framework.Bundle;
import org.osgi.service.condpermadmin.Condition;
import org.osgi.service.condpermadmin.ConditionInfo;

public class PostPonedCondition implements Condition {

	static public Condition getCondition(final Bundle bundle, final ConditionInfo info) {
		return new PostPonedCondition();
	}

	public boolean isMutable() {
		return true;
	}

	public boolean isPostponed() {
		return true;
	}

	public boolean isSatisfied() {
		return true;
	}

	public boolean isSatisfied(Condition[] conditions, Dictionary context) {
		return true;
	}

}
