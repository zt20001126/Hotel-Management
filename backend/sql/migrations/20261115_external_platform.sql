-- 第三方平台经营模块；已有数据库需单独执行本增量脚本。
SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS `external_platform` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '平台ID',
  `name` VARCHAR(50) NOT NULL COMMENT '平台名称',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '启用状态',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`), UNIQUE KEY `uk_external_platform_name` (`name`),
  CONSTRAINT `chk_external_platform_status` CHECK (`status` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='第三方平台';

CREATE TABLE IF NOT EXISTS `external_anchor` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主播ID',
  `name` VARCHAR(50) NOT NULL COMMENT '主播名称',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '启用状态',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`), CONSTRAINT `chk_external_anchor_status` CHECK (`status` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='第三方平台主播';

CREATE TABLE IF NOT EXISTS `external_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` VARCHAR(50) NOT NULL COMMENT '系统订单号',
  `platform_order_no` VARCHAR(100) DEFAULT NULL COMMENT '平台订单号',
  `platform_id` BIGINT NOT NULL COMMENT '平台ID',
  `anchor_id` BIGINT DEFAULT NULL COMMENT '主播ID',
  `room_id` BIGINT NOT NULL COMMENT '房间ID',
  `check_in_date` DATE NOT NULL COMMENT '计划入住日期',
  `check_out_date` DATE NOT NULL COMMENT '计划退房日期',
  `guest_count` INT NOT NULL COMMENT '入住人数',
  `total_amount` DECIMAL(12,2) NOT NULL COMMENT '成交金额',
  `platform_fee` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '平台手续费',
  `anchor_fee` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '主播手续费',
  `status` VARCHAR(20) NOT NULL DEFAULT 'BOOKED' COMMENT '订单状态',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`), UNIQUE KEY `uk_external_order_no` (`order_no`),
  UNIQUE KEY `uk_external_platform_order` (`platform_id`, `platform_order_no`),
  KEY `idx_external_order_room` (`room_id`, `status`, `check_in_date`, `check_out_date`),
  KEY `idx_external_order_platform` (`platform_id`, `check_in_date`),
  KEY `idx_external_order_anchor` (`anchor_id`, `check_in_date`),
  CONSTRAINT `fk_external_order_platform` FOREIGN KEY (`platform_id`) REFERENCES `external_platform` (`id`),
  CONSTRAINT `fk_external_order_anchor` FOREIGN KEY (`anchor_id`) REFERENCES `external_anchor` (`id`),
  CONSTRAINT `chk_external_order_dates` CHECK (`check_out_date` > `check_in_date`),
  CONSTRAINT `chk_external_order_guest_count` CHECK (`guest_count` > 0),
  CONSTRAINT `chk_external_order_amounts` CHECK (`total_amount` >= 0 AND `platform_fee` >= 0 AND `anchor_fee` >= 0),
  CONSTRAINT `chk_external_order_anchor_fee` CHECK (`anchor_id` IS NOT NULL OR `anchor_fee` = 0),
  CONSTRAINT `chk_external_order_status` CHECK (`status` IN ('BOOKED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='第三方平台订单';

INSERT INTO `external_platform` (`name`) VALUES ('抖音'), ('美团'), ('携程'), ('飞猪')
ON DUPLICATE KEY UPDATE `name` = `external_platform`.`name`;
