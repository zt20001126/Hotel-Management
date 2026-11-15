CREATE TABLE IF NOT EXISTS `operating_expenses` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '支出ID',
    `content` VARCHAR(500) NOT NULL COMMENT '支出内容',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '支出金额',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_created_at` (`created_at`),
    CONSTRAINT `chk_expense_amount` CHECK (`amount` > 0)
) COMMENT='经营支出表';
