import org.spockframework.runtime.model.parallel.ExecutionMode

runner {
    parallel {
        enabled Boolean.parseBoolean(System.getProperty("parallel", "true"))
        // Спеки — параллельно, фичи внутри спеки — последовательно:
        // драйвер Selenide thread-local, значит одна спека = один поток = один браузер за раз.
        defaultSpecificationExecutionMode = ExecutionMode.CONCURRENT
        defaultExecutionMode = ExecutionMode.SAME_THREAD
        fixed(Integer.getInteger("parallel.threads", 4))
    }
}
