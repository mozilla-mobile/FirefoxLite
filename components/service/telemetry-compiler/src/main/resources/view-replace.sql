CREATE OR REPLACE VIEW
    `moz-fx-data-shared-prod.telemetry.rocket_android_events_v1` AS
WITH base_events AS (

SELECT
  *,
  event.f0_ AS timestamp,
  event.f0_ AS event_timestamp,
  event.f1_ AS event_category,
  event.f2_ AS event_method,
  event.f3_ AS event_object,
  event.f4_ AS event_value,
  event.f5_ AS event_map_values,
  metadata.uri.app_version,
  osversion AS os_version,
  metadata.geo.country,
  metadata.geo.city,
  metadata.uri.app_name
FROM
  `moz-fx-data-shared-prod.telemetry.focus_event`
  CROSS JOIN UNNEST(events) AS event

), all_events AS (
SELECT
    submission_timestamp,
    client_id AS device_id,
    (created + COALESCE(SAFE_CAST(`moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'session_id') AS INT64), 0)) AS session_id,
    CASE
---REPLACE---ME---
    END AS event_name,
    event_timestamp AS timestamp,
    (event_timestamp + created) AS time,
    app_version,
    os AS os_name,
    os_version,
    country,
    city,
    (SELECT
      ARRAY_AGG(CONCAT('"',
        CAST(key AS STRING), '":"',
        CAST(value AS STRING), '"'))
     FROM
       UNNEST(event_map_values)) AS event_props_1,
    event_map_values,
    event_object,
    event_value,
    event_method,
    event_category,
    created,
    settings
FROM
    base_events
WHERE app_name IN ('Zerda', 'OTHER') AND os IN ('Android')
), all_events_with_insert_ids AS (
SELECT
  * EXCEPT (event_category, created),
  CONCAT(device_id, "-", CAST(created AS STRING), "-", SPLIT(event_name, " - ")[OFFSET(1)], "-", CAST(timestamp AS STRING), "-", event_category, "-", event_method, "-", event_object) AS insert_id,
  event_name AS event_type
FROM
  all_events
WHERE
  event_name IS NOT NULL
), extra_props AS (
SELECT
  * EXCEPT (event_map_values, event_object, event_value, event_method, event_name),
  (SELECT ARRAY_AGG(CONCAT('"', CAST(key AS STRING), '":"', CAST(value AS STRING), '"')) FROM (
      SELECT 'to' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'to') AS value
      UNION ALL SELECT 'on' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'on') AS value
      UNION ALL SELECT 'from' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'from') AS value
      UNION ALL SELECT 'mode' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'mode') AS value
      UNION ALL SELECT 'type' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'type') AS value
      UNION ALL SELECT 'source' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'source') AS value
      UNION ALL SELECT 'default' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'default') AS value
      UNION ALL SELECT 'position' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'position') AS value
      UNION ALL SELECT 'version' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'version') AS value
      UNION ALL SELECT 'category' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'category') AS value
      UNION ALL SELECT 'category_versio' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'category_versio') AS value
      UNION ALL SELECT 'snackbar' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'snackbar') AS value
      UNION ALL SELECT 'success' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'success') AS value
      UNION ALL SELECT 'delay' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'delay') AS value
      UNION ALL SELECT 'message' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'message') AS value
      UNION ALL SELECT 'engine' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'engine') AS value
      UNION ALL SELECT 'duration' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'duration') AS value
      UNION ALL SELECT 'from_build' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'from_build') AS value
      UNION ALL SELECT 'to_build' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'to_build') AS value
      UNION ALL SELECT 'action' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'action') AS value
      UNION ALL SELECT 'finish' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'finish') AS value
      UNION ALL SELECT 'page' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'page') AS value
      UNION ALL SELECT 'message_id' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'message_id') AS value
      UNION ALL SELECT 'link' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'link') AS value
      UNION ALL SELECT 'background' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'background') AS value
      UNION ALL SELECT 'primary' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'primary') AS value
      UNION ALL SELECT 'vertical' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'vertical') AS value
      UNION ALL SELECT 'component_id' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'component_id') AS value
      UNION ALL SELECT 'feed' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'feed') AS value
      UNION ALL SELECT 'subcategory_id' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'subcategory_id') AS value
      UNION ALL SELECT 'version_id' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'version_id') AS value
      UNION ALL SELECT 'app_link' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'app_link') AS value
      UNION ALL SELECT 'session_time' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'session_time') AS value
      UNION ALL SELECT 'show_keyboard' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'show_keyboard') AS value
      UNION ALL SELECT 'url_counts' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'url_counts') AS value
      UNION ALL SELECT 'impression' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'impression') AS value
      UNION ALL SELECT 'loadtime' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'loadtime') AS value
      UNION ALL SELECT 'audience_name' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'audience_name') AS value
      UNION ALL SELECT 'finished' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'finished') AS value
      UNION ALL SELECT 'task' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'task') AS value
      UNION ALL SELECT 'item_name' AS key, `moz-fx-data-shared-prod.udf.get_key`(event_map_values, 'item_name') AS value
  ) WHERE VALUE IS NOT NULL) AS event_props_2,
  ARRAY_CONCAT(ARRAY<STRING>[],
    (SELECT ARRAY_AGG(
    CASE
        WHEN key='pref_search_engine' THEN CONCAT('"', 'pref_search_engine', '":"', CAST(value AS STRING), '"')
        WHEN key='pref_privacy_turbo_mode' THEN CONCAT('"', 'pref_privacy_turbo_mode', '":', CAST(SAFE_CAST(value AS BOOLEAN) AS STRING))
        WHEN key='pref_performance_block_images' THEN CONCAT('"', 'pref_performance_block_images', '":', CAST(SAFE_CAST(value AS BOOLEAN) AS STRING))
        WHEN key='pref_default_browser' THEN CONCAT('"', 'pref_default_browser', '":', CAST(SAFE_CAST(value AS BOOLEAN) AS STRING))
        WHEN key='pref_save_downloads_to' THEN CONCAT('"', 'pref_save_downloads_to', '":"', CAST(value AS STRING), '"')
        WHEN key='pref_webview_version' THEN CONCAT('"', 'pref_webview_version', '":"', CAST(value AS STRING), '"')
        WHEN key='install_referrer' THEN CONCAT('"', 'install_referrer', '":"', CAST(value AS STRING), '"')
        WHEN key='experiment_name' THEN CONCAT('"', 'experiment_name', '":"', CAST(value AS STRING), '"')
        WHEN key='experiment_bucket' THEN CONCAT('"', 'experiment_bucket', '":"', CAST(value AS STRING), '"')
        WHEN key='pref_locale' THEN CONCAT('"', 'pref_locale', '":"', CAST(value AS STRING), '"')
        WHEN key='pref_key_s_tracker_token' THEN CONCAT('"', 'pref_key_s_tracker_token', '":"', CAST(value AS STRING), '"')
    END
    IGNORE NULLS)
  FROM
    UNNEST(SETTINGS)
  )) AS user_props
FROM
  all_events_with_insert_ids
)

SELECT
  * EXCEPT (event_props_1, event_props_2, user_props, settings),
  CONCAT('{', ARRAY_TO_STRING((
   SELECT ARRAY_AGG(DISTINCT e) FROM UNNEST(ARRAY_CONCAT(IFNULL(event_props_1, []), IFNULL(event_props_2, []))) AS e
  ), ","), '}') AS event_properties,
  CONCAT('{', ARRAY_TO_STRING(user_props, ","), '}') AS user_properties
FROM extra_props