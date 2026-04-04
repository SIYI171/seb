/*
Navicat MySQL Data Transfer

Source Server         : seb
Source Server Version : 80024
Source Host           : hk3.lleft.cn:3306
Source Database       : siyi_seb

Target Server Type    : MYSQL
Target Server Version : 80024
File Encoding         : 65001

Date: 2026-04-04 16:10:50
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for admin
-- ----------------------------
DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password` varchar(255) NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for pageview
-- ----------------------------
DROP TABLE IF EXISTS `pageview`;
CREATE TABLE `pageview` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `website_id` bigint NOT NULL,
  `session_id` varchar(64) NOT NULL,
  `url` varchar(500) NOT NULL,
  `referrer` varchar(500) DEFAULT NULL,
  `browser` varchar(50) DEFAULT NULL,
  `os` varchar(50) DEFAULT NULL,
  `device` varchar(20) DEFAULT NULL,
  `country` varchar(50) DEFAULT NULL,
  `ip` varchar(45) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_website_id` (`website_id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_website_created` (`website_id`,`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for session
-- ----------------------------
DROP TABLE IF EXISTS `session`;
CREATE TABLE `session` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `website_id` bigint NOT NULL,
  `visitor_id` varchar(64) NOT NULL,
  `entry_url` varchar(500) DEFAULT NULL,
  `exit_url` varchar(500) DEFAULT NULL,
  `duration` int DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_website_id` (`website_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for website
-- ----------------------------
DROP TABLE IF EXISTS `website`;
CREATE TABLE `website` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `domain` varchar(255) NOT NULL,
  `tracking_id` varchar(64) NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `share_token` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `tracking_id` (`tracking_id`),
  UNIQUE KEY `share_token` (`share_token`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
