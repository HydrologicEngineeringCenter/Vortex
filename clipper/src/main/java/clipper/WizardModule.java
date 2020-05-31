package clipper;

import com.google.inject.AbstractModule;

public class WizardModule extends AbstractModule {
    @Override
    protected void configure() {
        WizardData model = new WizardData();
        bind(WizardData.class).toInstance(model);
    }
}

