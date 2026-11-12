-- H2 MODE=MySQL / MySQL 8.0 通用建表语句。应用启动自动执行，重复启动不删除已有数据。

CREATE TABLE IF NOT EXISTS app_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(255) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  role VARCHAR(255),
  nickname VARCHAR(255),
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP,
  last_login_at TIMESTAMP,
  login_count BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS orchard (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  owner_id BIGINT,
  name VARCHAR(255),
  location VARCHAR(255),
  area DOUBLE,
  variety VARCHAR(255),
  image_url VARCHAR(255),
  fruiting BOOLEAN
);

CREATE TABLE IF NOT EXISTS device (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  orchard_id BIGINT,
  device_sn VARCHAR(255) NOT NULL UNIQUE,
  location VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS sensor_data (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  device_id BIGINT,
  orchard_id BIGINT,
  temperature DOUBLE,
  humidity DOUBLE,
  soil_humidity DOUBLE,
  light DOUBLE,
  co2 DOUBLE,
  collect_time TIMESTAMP,
  received_time TIMESTAMP,
  risk_level VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS risk_warning (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  orchard_id BIGINT,
  sensor_data_id BIGINT,
  pest_type VARCHAR(255),
  risk_level VARCHAR(255),
  reason VARCHAR(4000),
  suggestion VARCHAR(4000),
  created_at TIMESTAMP,
  handled BOOLEAN
);

CREATE TABLE IF NOT EXISTS observation (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  orchard_id BIGINT,
  pest_type VARCHAR(255),
  severity VARCHAR(255),
  description VARCHAR(4000),
  image_url VARCHAR(255),
  suggestion VARCHAR(4000),
  created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS threshold_config (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  orchard_id BIGINT NOT NULL UNIQUE,
  temperature_min DOUBLE,
  temperature_max DOUBLE,
  humidity_max DOUBLE,
  soil_humidity_max DOUBLE,
  light_min DOUBLE,
  light_max DOUBLE,
  co2_max DOUBLE,
  brown_temp_min DOUBLE,
  brown_temp_max DOUBLE,
  brown_humidity DOUBLE,
  fly_temp_min DOUBLE,
  fly_temp_max DOUBLE,
  fly_humidity DOUBLE,
  cancer_humidity DOUBLE,
  cancer_soil DOUBLE
);

CREATE TABLE IF NOT EXISTS control_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  device_id BIGINT,
  orchard_id BIGINT,
  device_sn VARCHAR(255) NOT NULL,
  control_command VARCHAR(255),
  operator_name VARCHAR(255),
  created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS announcement (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(255),
  content VARCHAR(4000),
  created_at TIMESTAMP
);
