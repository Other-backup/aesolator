# Black Diamond Ae.solator App Byte Reduction

- generated_at_utc: `2026-04-19T11:36:39Z`
- frontier: `/data/data/com.termux/files/home/.cache/omega-donor-frontier/20260419T103446Z-multi-donor-aesolator-online-rerun2/multi-donor-frontier.json`
- resolution_ledger: `/data/data/com.termux/files/home/aesolator/docs/BLACK_DIAMOND_DONOR_LEDGER_20260419.json`
- donors_total: `214`
- donors_read: `204`
- records_examined: `141138`
- records_considered: `141138`
- raw_candidate_paths: `41944`
- logic_candidate_paths: `1049`
- high_signal_logic_candidate_paths: `920`
- unledgered_high_signal_logic_candidate_paths: `0`
- ledger_covered_high_signal_logic_candidate_paths: `920`
- missing_record_files: `0`

## Category Counts

| category | paths | high_signal_paths |
| --- | ---: | ---: |
| misc_app_delta | 39095 | 36518 |
| low_signal_asset_or_doc | 1201 | 947 |
| java_app_general | 407 | 382 |
| java_runtime_routing | 403 | 357 |
| runtime_asset_payload | 306 | 259 |
| native_cpp_general | 192 | 173 |
| ui_resource_contract | 169 | 162 |
| native_graphics_cpp | 120 | 119 |
| build_config_contract | 51 | 23 |

## Top Unledgered High-Signal Logic Candidates

| path | category | scope | donors | high_signal | same_blob | score | high_signal_samples |
| --- | --- | --- | ---: | ---: | ---: | ---: | --- |

## Interpretation

- This report is byte-to-byte donor-side comparison against `aesolator`, with local-only noise omitted.
- Logic lanes are Java/Kotlin runtime routing, native graphics C/C++, build/config, runtime assets, and UI resource contracts.
- Low-signal assets and docs are retained in JSON for audit but must not drive source transfer before logic lanes.
- No donor code is accepted by this report; it only defines the next arbitration backlog.
