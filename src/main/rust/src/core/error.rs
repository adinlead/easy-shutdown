use std::fmt;

/// 核心层统一错误（对应 Java 版 `OperationException`）：
/// 底层错误包装后向上返回，由 UI 边界统一处理并给出用户可见反馈。
#[derive(Debug)]
pub struct OperationError {
    message: String,
    source: Option<Box<dyn std::error::Error + Send + Sync>>,
}

impl OperationError {
    pub fn new(message: impl Into<String>) -> Self {
        Self { message: message.into(), source: None }
    }

    pub fn with_source(
        message: impl Into<String>,
        source: impl Into<Box<dyn std::error::Error + Send + Sync>>,
    ) -> Self {
        Self { message: message.into(), source: Some(source.into()) }
    }
}

impl fmt::Display for OperationError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.message)
    }
}

impl std::error::Error for OperationError {
    fn source(&self) -> Option<&(dyn std::error::Error + 'static)> {
        self.source.as_deref().map(|e| e as &(dyn std::error::Error + 'static))
    }
}
