package io.qa.ui.spock;

import io.qa.ui.core.diagnostics.Diagnostics;
import org.spockframework.runtime.extension.IGlobalExtension;
import org.spockframework.runtime.model.SpecInfo;

/**
 * Глобальный extension: на падении любой фичи прикладывает скриншот, page source и console log.
 * try/catch здесь инфраструктурный — исключение всегда пробрасывается дальше.
 */
public class FailureArtifactsExtension implements IGlobalExtension {

    @Override
    public void visitSpec(SpecInfo spec) {
        spec.getAllFeatures().forEach(feature ->
                feature.getFeatureMethod().addInterceptor(invocation -> {
                    try {
                        invocation.proceed();
                    } catch (Throwable failure) {
                        Diagnostics.captureFailure(spec.getName() + " :: " + feature.getName());
                        throw failure;
                    }
                }));
    }
}
