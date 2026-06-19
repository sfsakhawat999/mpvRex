// Astra, a tone mapping operator designed to preserve the creator's intent

//!PARAM PTS
//!TYPE float
0.0

//!PARAM min_luma
//!TYPE float
0.0

//!PARAM max_luma
//!TYPE float
0.0

//!PARAM max_cll
//!TYPE float
0.0

//!PARAM max_fall
//!TYPE float
0.0

//!PARAM scene_max_r
//!TYPE float
0.0

//!PARAM scene_max_g
//!TYPE float
0.0

//!PARAM scene_max_b
//!TYPE float
0.0

//!PARAM scene_avg
//!TYPE float
0.0

//!PARAM max_pq_y
//!TYPE float
0.0

//!PARAM avg_pq_y
//!TYPE float
0.0

//!PARAM reference_white
//!TYPE float
//!MINIMUM 0.0
//!MAXIMUM 1000.0
203.0

//!PARAM contrast_ratio
//!TYPE float
//!MINIMUM 10.0
//!MAXIMUM 100000000.0
1000.0

//!PARAM auto_exposure_anchor
//!TYPE float
//!MINIMUM 0.0
//!MAXIMUM 1.0
0.6

//!PARAM auto_exposure_limit_negtive
//!TYPE float
//!MINIMUM 0.0
//!MAXIMUM 5.0
2.3

//!PARAM auto_exposure_limit_postive
//!TYPE float
//!MINIMUM 0.0
//!MAXIMUM 5.0
0.0

//!PARAM shadow_weight
//!TYPE float
//!MINIMUM 0.0
//!MAXIMUM 1.0
0.4

//!PARAM highlight_weight
//!TYPE float
//!MINIMUM 0.0
//!MAXIMUM 1.0
0.1

//!PARAM highlight_overshoot
//!TYPE float
//!MINIMUM 0.0
//!MAXIMUM 2.0
1.0

//!PARAM contrast_bias
//!TYPE float
//!MINIMUM -1.0
//!MAXIMUM  1.0
0.0

//!PARAM hk_effect_compensate_scaling
//!TYPE float
//!MINIMUM 0.0
//!MAXIMUM 1.0
1.0

//!PARAM chroma_correction_scaling
//!TYPE float
//!MINIMUM 0.0
//!MAXIMUM 5.0
1.0

//!PARAM chroma_correction_rate
//!TYPE float
//!MINIMUM 0.0
//!MAXIMUM 5.0
1.0

//!PARAM chroma_correction_threshold
//!TYPE float
//!MINIMUM 0.0
//!MAXIMUM 1.0
0.0

//!PARAM spatial_stable_iterations
//!TYPE uint
//!MINIMUM 0
//!MAXIMUM 8
2

//!PARAM temporal_stable_window
//!TYPE float
//!MINIMUM 0.0
//!MAXIMUM 2.0
0.33

//!PARAM enable_metering
//!TYPE uint
//!MINIMUM 0
//!MAXIMUM 2
2

//!PARAM preview_metering
//!TYPE uint
//!MINIMUM 0
//!MAXIMUM 1
0

//!BUFFER METERED
//!VAR uint metered_max_i
//!VAR uint metered_min_i
//!VAR uint metered_avg_i
//!STORAGE

//!BUFFER METERED_TEMPORAL
//!VAR uint metered_max_i_t[256]
//!VAR uint metered_min_i_t[256]
//!VAR uint metered_avg_i_t[256]
//!VAR uint metered_pts_t[256]
//!STORAGE

//!BUFFER METERED_SMOOTHED
//!VAR uint smoothed_max_i
//!VAR uint smoothed_min_i
//!VAR uint smoothed_avg_i
//!STORAGE

//!BUFFER METADATA
//!VAR float max_i
//!VAR float min_i
//!VAR float avg_i
//!VAR float ev
//!STORAGE

//!HOOK OUTPUT
//!BIND HOOKED
//!SAVE METERING
//!COMPONENTS 1
//!WHEN enable_metering 0 > max_pq_y 0 = * scene_max_r 0 = * scene_max_g 0 = * scene_max_b 0 = *
//!DESC metering (intensity map)

const float m1 = 2610.0 / 4096.0 / 4.0;
const float m2 = 2523.0 / 4096.0 * 128.0;
const float c1 = 3424.0 / 4096.0;
const float c2 = 2413.0 / 4096.0 * 32.0;
const float c3 = 2392.0 / 4096.0 * 32.0;
const float pw = 10000.0;

float pq_eotf_inv(float x) {
    float t = pow(x / pw, m1);
    return pow((c1 + c2 * t) / (1.0 + c3 * t), m2);
}

float RGB_to_Y(vec3 rgb) {
    const vec3 coefficients = vec3(0.2627002120112671, 0.6779980715188708, 0.05930171646986196);
    return dot(rgb, coefficients);
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);
    float y = RGB_to_Y(color.rgb);
    float y_abs = y * reference_white;
    float intensity = pq_eotf_inv(y_abs);
    return vec4(vec3(intensity), 1.0);
}

//!HOOK OUTPUT
//!BIND METERING
//!SAVE METERING
//!WIDTH 512
//!HEIGHT 288
//!DESC metering (spatial stabilization, downscaling)

vec4 hook() {
    return METERING_tex(METERING_pos);
}

//!HOOK OUTPUT
//!BIND METERING
//!SAVE METERING
//!WHEN spatial_stable_iterations 0 >
//!DESC metering (spatial stabilization, blur, horizonal)

// Efficient Gaussian blur with linear sampling
// by Daniel Rákos
// https://www.rastergrid.com/blog/2010/09/efficient-gaussian-blur-with-linear-sampling/

const vec3 offset = vec3(0.0000000000, 1.3846153846, 3.2307692308);
const vec3 weight = vec3(0.2270270270, 0.3162162162, 0.0702702703);
const vec2 direction = vec2(1.0, 0.0);

vec4 hook() {
    vec4 c = METERING_tex(METERING_pos) * weight[0];
    for (uint i = 1; i < 3; i++) {
        c += METERING_texOff( direction * offset[i]) * weight[i];
        c += METERING_texOff(-direction * offset[i]) * weight[i];
    }
    return c;
}

//!HOOK OUTPUT
//!BIND METERING
//!SAVE METERING
//!WHEN spatial_stable_iterations 0 >
//!DESC metering (spatial stabilization, blur, vertical)

const vec3 offset = vec3(0.0000000000, 1.3846153846, 3.2307692308);
const vec3 weight = vec3(0.2270270270, 0.3162162162, 0.0702702703);
const vec2 direction = vec2(0.0, 1.0);

vec4 hook() {
    vec4 c = METERING_tex(METERING_pos) * weight[0];
    for (uint i = 1; i < 3; i++) {
        c += METERING_texOff( direction * offset[i]) * weight[i];
        c += METERING_texOff(-direction * offset[i]) * weight[i];
    }
    return c;
}

//!HOOK OUTPUT
//!BIND METERING
//!SAVE METERING
//!WHEN spatial_stable_iterations 1 >
//!DESC metering (spatial stabilization, blur, horizonal)

const vec3 offset = vec3(0.0000000000, 1.3846153846, 3.2307692308);
const vec3 weight = vec3(0.2270270270, 0.3162162162, 0.0702702703);
const vec2 direction = vec2(1.0, 0.0);

vec4 hook() {
    vec4 c = METERING_tex(METERING_pos) * weight[0];
    for (uint i = 1; i < 3; i++) {
        c += METERING_texOff( direction * offset[i]) * weight[i];
        c += METERING_texOff(-direction * offset[i]) * weight[i];
    }
    return c;
}

//!HOOK OUTPUT
//!BIND METERING
//!SAVE METERING
//!WHEN spatial_stable_iterations 1 >
//!DESC metering (spatial stabilization, blur, vertical)

const vec3 offset = vec3(0.0000000000, 1.3846153846, 3.2307692308);
const vec3 weight = vec3(0.2270270270, 0.3162162162, 0.0702702703);
const vec2 direction = vec2(0.0, 1.0);

vec4 hook() {
    vec4 c = METERING_tex(METERING_pos) * weight[0];
    for (uint i = 1; i < 3; i++) {
        c += METERING_texOff( direction * offset[i]) * weight[i];
        c += METERING_texOff(-direction * offset[i]) * weight[i];
    }
    return c;
}

//!HOOK OUTPUT
//!BIND METERING
//!SAVE METERING
//!WHEN spatial_stable_iterations 2 >
//!DESC metering (spatial stabilization, blur, horizonal)

const vec3 offset = vec3(0.0000000000, 1.3846153846, 3.2307692308);
const vec3 weight = vec3(0.2270270270, 0.3162162162, 0.0702702703);
const vec2 direction = vec2(1.0, 0.0);

vec4 hook() {
    vec4 c = METERING_tex(METERING_pos) * weight[0];
    for (uint i = 1; i < 3; i++) {
        c += METERING_texOff( direction * offset[i]) * weight[i];
        c += METERING_texOff(-direction * offset[i]) * weight[i];
    }
    return c;
}

//!HOOK OUTPUT
//!BIND METERING
//!SAVE METERING
//!WHEN spatial_stable_iterations 2 >
//!DESC metering (spatial stabilization, blur, vertical)

const vec3 offset = vec3(0.0000000000, 1.3846153846, 3.2307692308);
const vec3 weight = vec3(0.2270270270, 0.3162162162, 0.0702702703);
const vec2 direction = vec2(0.0, 1.0);

vec4 hook() {
    vec4 c = METERING_tex(METERING_pos) * weight[0];
    for (uint i = 1; i < 3; i++) {
        c += METERING_texOff( direction * offset[i]) * weight[i];
        c += METERING_texOff(-direction * offset[i]) * weight[i];
    }
    return c;
}

//!HOOK OUTPUT
//!BIND METERING
//!SAVE METERING
//!WHEN spatial_stable_iterations 3 >
//!DESC metering (spatial stabilization, blur, horizonal)

const vec3 offset = vec3(0.0000000000, 1.3846153846, 3.2307692308);
const vec3 weight = vec3(0.2270270270, 0.3162162162, 0.0702702703);
const vec2 direction = vec2(1.0, 0.0);

vec4 hook() {
    vec4 c = METERING_tex(METERING_pos) * weight[0];
    for (uint i = 1; i < 3; i++) {
        c += METERING_texOff( direction * offset[i]) * weight[i];
        c += METERING_texOff(-direction * offset[i]) * weight[i];
    }
    return c;
}

//!HOOK OUTPUT
//!BIND METERING
//!SAVE METERING
//!WHEN spatial_stable_iterations 3 >
//!DESC metering (spatial stabilization, blur, vertical)

const vec3 offset = vec3(0.0000000000, 1.3846153846, 3.2307692308);
const vec3 weight = vec3(0.2270270270, 0.3162162162, 0.0702702703);
const vec2 direction = vec2(0.0, 1.0);

vec4 hook() {
    vec4 c = METERING_tex(METERING_pos) * weight[0];
    for (uint i = 1; i < 3; i++) {
        c += METERING_texOff( direction * offset[i]) * weight[i];
        c += METERING_texOff(-direction * offset[i]) * weight[i];
    }
    return c;
}

//!HOOK OUTPUT
//!BIND METERING
//!SAVE METERING
//!WHEN spatial_stable_iterations 4 >
//!DESC metering (spatial stabilization, blur, horizonal)

const vec3 offset = vec3(0.0000000000, 1.3846153846, 3.2307692308);
const vec3 weight = vec3(0.2270270270, 0.3162162162, 0.0702702703);
const vec2 direction = vec2(1.0, 0.0);

vec4 hook() {
    vec4 c = METERING_tex(METERING_pos) * weight[0];
    for (uint i = 1; i < 3; i++) {
        c += METERING_texOff( direction * offset[i]) * weight[i];
        c += METERING_texOff(-direction * offset[i]) * weight[i];
    }
    return c;
}

//!HOOK OUTPUT
//!BIND METERING
//!SAVE METERING
//!WHEN spatial_stable_iterations 4 >
//!DESC metering (spatial stabilization, blur, vertical)

const vec3 offset = vec3(0.0000000000, 1.3846153846, 3.2307692308);
const vec3 weight = vec3(0.2270270270, 0.3162162162, 0.0702702703);
const vec2 direction = vec2(0.0, 1.0);

vec4 hook() {
    vec4 c = METERING_tex(METERING_pos) * weight[0];
    for (uint i = 1; i < 3; i++) {
        c += METERING_texOff( direction * offset[i]) * weight[i];
        c += METERING_texOff(-direction * offset[i]) * weight[i];
    }
    return c;
}

//!HOOK OUTPUT
//!BIND METERING
//!SAVE METERING
//!WHEN spatial_stable_iterations 5 >
//!DESC metering (spatial stabilization, blur, horizonal)

const vec3 offset = vec3(0.0000000000, 1.3846153846, 3.2307692308);
const vec3 weight = vec3(0.2270270270, 0.3162162162, 0.0702702703);
const vec2 direction = vec2(1.0, 0.0);

vec4 hook() {
    vec4 c = METERING_tex(METERING_pos) * weight[0];
    for (uint i = 1; i < 3; i++) {
        c += METERING_texOff( direction * offset[i]) * weight[i];
        c += METERING_texOff(-direction * offset[i]) * weight[i];
    }
    return c;
}

//!HOOK OUTPUT
//!BIND METERING
//!SAVE METERING
//!WHEN spatial_stable_iterations 5 >
//!DESC metering (spatial stabilization, blur, vertical)

const vec3 offset = vec3(0.0000000000, 1.3846153846, 3.2307692308);
const vec3 weight = vec3(0.2270270270, 0.3162162162, 0.0702702703);
const vec2 direction = vec2(0.0, 1.0);

vec4 hook() {
    vec4 c = METERING_tex(METERING_pos) * weight[0];
    for (uint i = 1; i < 3; i++) {
        c += METERING_texOff( direction * offset[i]) * weight[i];
        c += METERING_texOff(-direction * offset[i]) * weight[i];
    }
    return c;
}

//!HOOK OUTPUT
//!BIND METERING
//!SAVE METERING
//!WHEN spatial_stable_iterations 6 >
//!DESC metering (spatial stabilization, blur, horizonal)

const vec3 offset = vec3(0.0000000000, 1.3846153846, 3.2307692308);
const vec3 weight = vec3(0.2270270270, 0.3162162162, 0.0702702703);
const vec2 direction = vec2(1.0, 0.0);

vec4 hook() {
    vec4 c = METERING_tex(METERING_pos) * weight[0];
    for (uint i = 1; i < 3; i++) {
        c += METERING_texOff( direction * offset[i]) * weight[i];
        c += METERING_texOff(-direction * offset[i]) * weight[i];
    }
    return c;
}

//!HOOK OUTPUT
//!BIND METERING
//!SAVE METERING
//!WHEN spatial_stable_iterations 6 >
//!DESC metering (spatial stabilization, blur, vertical)

const vec3 offset = vec3(0.0000000000, 1.3846153846, 3.2307692308);
const vec3 weight = vec3(0.2270270270, 0.3162162162, 0.0702702703);
const vec2 direction = vec2(0.0, 1.0);

vec4 hook() {
    vec4 c = METERING_tex(METERING_pos) * weight[0];
    for (uint i = 1; i < 3; i++) {
        c += METERING_texOff( direction * offset[i]) * weight[i];
        c += METERING_texOff(-direction * offset[i]) * weight[i];
    }
    return c;
}

//!HOOK OUTPUT
//!BIND METERING
//!SAVE METERING
//!WHEN spatial_stable_iterations 7 >
//!DESC metering (spatial stabilization, blur, horizonal)

const vec3 offset = vec3(0.0000000000, 1.3846153846, 3.2307692308);
const vec3 weight = vec3(0.2270270270, 0.3162162162, 0.0702702703);
const vec2 direction = vec2(1.0, 0.0);

vec4 hook() {
    vec4 c = METERING_tex(METERING_pos) * weight[0];
    for (uint i = 1; i < 3; i++) {
        c += METERING_texOff( direction * offset[i]) * weight[i];
        c += METERING_texOff(-direction * offset[i]) * weight[i];
    }
    return c;
}

//!HOOK OUTPUT
//!BIND METERING
//!SAVE METERING
//!WHEN spatial_stable_iterations 7 >
//!DESC metering (spatial stabilization, blur, vertical)

const vec3 offset = vec3(0.0000000000, 1.3846153846, 3.2307692308);
const vec3 weight = vec3(0.2270270270, 0.3162162162, 0.0702702703);
const vec2 direction = vec2(0.0, 1.0);

vec4 hook() {
    vec4 c = METERING_tex(METERING_pos) * weight[0];
    for (uint i = 1; i < 3; i++) {
        c += METERING_texOff( direction * offset[i]) * weight[i];
        c += METERING_texOff(-direction * offset[i]) * weight[i];
    }
    return c;
}

//!HOOK OUTPUT
//!BIND METERING
//!BIND METERED
//!SAVE EMPTY
//!WIDTH 1
//!HEIGHT 1
//!COMPUTE 1 1
//!DESC metering (max, min, init)

void hook() {
    metered_max_i = 0;
    metered_min_i = 4095;
}

//!HOOK OUTPUT
//!BIND METERING
//!BIND METERED
//!SAVE EMPTY
//!COMPUTE 32 32
//!DESC metering (max, min)

shared uint smax[1024];
shared uint smin[1024];

uint to_uint(float x) {
    return uint(x * 4095.0 + 0.5);
}

void hook() {
    float value = METERING_tex(METERING_pos).x;
    uint rounded = to_uint(value);

    uint tid = gl_LocalInvocationIndex;
    smax[tid] = rounded;
    smin[tid] = rounded;

    barrier();

    for (uint s = 512; s > 0; s >>= 1) {
        if (tid < s) {
            smax[tid] = max(smax[tid], smax[tid + s]);
            smin[tid] = min(smin[tid], smin[tid + s]);
        }
        barrier();
    }

    if (tid == 0) {
        atomicMax(metered_max_i, smax[0]);
        atomicMin(metered_min_i, smin[0]);
    }
}

//!HOOK OUTPUT
//!BIND METERING
//!SAVE AVG
//!COMPONENTS 1
//!WIDTH 256
//!HEIGHT 256
//!WHEN auto_exposure_anchor 0 > enable_metering 1 > * avg_pq_y 0 = * scene_avg 0 = *
//!DESC metering (avg, 256, center-weighted)

vec2 map_coords(vec2 uv, float strength) {
    if (strength < 0.001) {
        return uv;
    }

    vec2 centered_uv = uv - vec2(0.5);
    float radius = length(centered_uv);

    if (radius == 0.0) {
        return vec2(0.5);
    }

    float distorted_radius  = tan(radius * strength) / strength;
    vec2 distorted_centered_uv  = normalize(centered_uv ) * distorted_radius;

    distorted_centered_uv = distorted_centered_uv / max(strength, 1.0);

    vec2 distorted_uv = distorted_centered_uv + vec2(0.5);

    vec2 kaleidoscope_uv = 1.0 - abs(fract(distorted_uv * 0.5) * 2.0 - 1.0);

    return kaleidoscope_uv;
}

vec2 map_coords(vec2 uv) {
    return map_coords(uv, 2.0);
}

vec4 hook() {
    return METERING_tex(map_coords(METERING_pos));
}

//!HOOK OUTPUT
//!BIND AVG
//!SAVE AVG
//!WIDTH AVG.w 2 /
//!HEIGHT AVG.h 2 /
//!DESC metering (avg, 128)
vec4 hook() { return AVG_tex(AVG_pos); }

//!HOOK OUTPUT
//!BIND AVG
//!SAVE AVG
//!WIDTH AVG.w 2 /
//!HEIGHT AVG.h 2 /
//!DESC metering (avg, 64)
vec4 hook() { return AVG_tex(AVG_pos); }

//!HOOK OUTPUT
//!BIND AVG
//!SAVE AVG
//!WIDTH AVG.w 2 /
//!HEIGHT AVG.h 2 /
//!DESC metering (avg, 32)
vec4 hook() { return AVG_tex(AVG_pos); }

//!HOOK OUTPUT
//!BIND AVG
//!SAVE AVG
//!WIDTH AVG.w 2 /
//!HEIGHT AVG.h 2 /
//!DESC metering (avg, 16)
vec4 hook() { return AVG_tex(AVG_pos); }

//!HOOK OUTPUT
//!BIND AVG
//!SAVE AVG
//!WIDTH AVG.w 2 /
//!HEIGHT AVG.h 2 /
//!DESC metering (avg, 8)
vec4 hook() { return AVG_tex(AVG_pos); }

//!HOOK OUTPUT
//!BIND AVG
//!SAVE AVG
//!WIDTH AVG.w 2 /
//!HEIGHT AVG.h 2 /
//!DESC metering (avg, 4)
vec4 hook() { return AVG_tex(AVG_pos); }

//!HOOK OUTPUT
//!BIND AVG
//!SAVE AVG
//!WIDTH AVG.w 2 /
//!HEIGHT AVG.h 2 /
//!DESC metering (avg, 2)
vec4 hook() { return AVG_tex(AVG_pos); }

//!HOOK OUTPUT
//!BIND AVG
//!BIND METERED
//!SAVE AVG
//!WIDTH 1
//!HEIGHT 1
//!COMPUTE 1 1
//!DESC metering (avg)

uint to_uint(float x) {
    return uint(x * 4095.0 + 0.5);
}

void hook() {
    metered_avg_i = to_uint(AVG_tex(AVG_pos).x);
}

//!HOOK OUTPUT
//!BIND METERING
//!BIND METERED
//!BIND METERED_TEMPORAL
//!BIND METERED_SMOOTHED
//!SAVE EMPTY
//!WIDTH 1
//!HEIGHT 1
//!COMPUTE 1 1
//!WHEN temporal_stable_window 0.0 >
//!DESC metering (temporal stabilization)

// ============================================================================
// TEMPORAL STABILIZATION - Configuration Parameters
// ============================================================================
// These parameters control the temporal smoothing behavior to reduce flicker
// while maintaining responsiveness to actual scene changes.

// Exponential decay factor for weighted moving average
// Range: 0.7-0.95. Lower = more smoothing but slower response
// Default: 0.85 balances smoothness and responsiveness
const float TEMPORAL_DECAY = 0.85;

// EMA (Exponential Moving Average) smoothing factor
// Range: 0.1-0.3. Lower = smoother but less responsive
// Default: 0.2 provides good stability without excessive lag
const float TEMPORAL_EMA_ALPHA = 0.2;

// Blend factor for gradual scene transition
// Range: 0.3-0.7. Lower = smoother transitions during scene cuts
// Default: 0.5 provides balanced transition speed
const float TEMPORAL_SCENE_BLEND = 0.5;

// Scene change blend factor (applied when cut is detected)
// Range: 0.2-0.5. Lower = smoother but may blur real scene changes
// Default: 0.3 maintains some smoothness during cuts
const float TEMPORAL_CUT_BLEND = 0.3;

// Base tolerance for scene change detection (in ΔE units)
// Range: 20.0-50.0. Higher = fewer false detections but may miss real cuts
// Default: 36.0 provides good balance for most content
const float TEMPORAL_BASE_TOLERANCE = 36.0;

// Adaptive tolerance scaling based on brightness
// Range: 0.3-0.7. Higher = more tolerance for bright scenes
// Default: 0.5 adapts well to various brightness levels
const float TEMPORAL_ADAPTIVE_SCALE = 0.5;

// Black scene threshold (below this is considered pure black)
// Range: ~0.002-0.008 (normalized). Higher = more aggressive black detection
// Default: 16/4095 catches most black frames without false positives
const float TEMPORAL_BLACK_THRESHOLD = 16.0 / 4095.0;

// Metric weights for scene change detection
// These weights determine the relative importance of each metric
// Total should sum to 1.0 for balanced detection
const float TEMPORAL_WEIGHT_AVG = 0.50; // Average is most reliable
const float TEMPORAL_WEIGHT_MAX = 0.35; // Maximum is important for highlights
const float TEMPORAL_WEIGHT_MIN = 0.15; // Minimum is least reliable (noise)

// Delta scale for converting normalized differences to perceptual units
// This converts [0,1] differences to ΔE-like perceptual differences
const float TEMPORAL_DELTA_SCALE = 720.0;

// Metric type identifiers for array access
const int METRIC_MAX = 0;
const int METRIC_MIN = 1;
const int METRIC_AVG = 2;

float to_float(uint x) {
    return float(x) / 4095.0;
}

uint to_uint(float x) {
    return uint(x * 4095.0 + 0.5);
}

float pts_to_float(uint x) {
    return uintBitsToFloat(x);
}

uint pts_to_uint(float x) {
    return floatBitsToUint(x);
}

// ============================================================================
// TEMPORAL STABILIZATION - Core Functions
// ============================================================================

const uint TEMPORAL_BUFFER_SIZE = 256u;

/**
 * Count how many frames in the history buffer fall within the time window
 * Uses PTS to determine temporal distance instead of fixed frame count
 */
uint temporal_frame_count() {
    float current_pts = PTS;
    uint count = 0u;
    for (uint i = 0u; i < TEMPORAL_BUFFER_SIZE; i++) {
        float dt = current_pts - pts_to_float(metered_pts_t[i]);
        if (dt >= 0.0 && dt <= temporal_stable_window)
            count = i + 1u;
        else
            break;
    }
    return max(count, 1u);
}

/**
 * Prepends current frame values to temporal history arrays
 * Maintains a sliding window of the last N frames for all three metrics
 * Also stores the PTS for time-based window calculation
 */
void temporal_prepend() {
    // Shift all historical values one position forward
    for (uint i = TEMPORAL_BUFFER_SIZE - 1u; i > 0u; i--) {
        metered_max_i_t[i] = metered_max_i_t[i - 1u];
        metered_min_i_t[i] = metered_min_i_t[i - 1u];
        metered_avg_i_t[i] = metered_avg_i_t[i - 1u];
        metered_pts_t[i]   = metered_pts_t[i - 1u];
    }

    // Insert current frame values at position 0
    metered_max_i_t[0] = metered_max_i;
    metered_min_i_t[0] = metered_min_i;
    metered_avg_i_t[0] = metered_avg_i;
    metered_pts_t[0]   = pts_to_uint(PTS);
}

/**
 * Calculates weighted moving average with exponential decay
 * Recent frames have higher weight than older frames
 *
 * @param type Metric type: METRIC_MAX, METRIC_MIN, or METRIC_AVG
 * @param count Number of frames within the time window
 * @return Weighted average value
 */
float temporal_weighted_mean(int type, uint count) {
    float sum_inv_weighted = 0.0;
    float sum_weights = 0.0;

    for (uint i = 0u; i < count; i++) {
        // Select appropriate buffer based on metric type
        float current;
        if (type == METRIC_MAX) {
            current = to_float(metered_max_i_t[i]);
        } else if (type == METRIC_MIN) {
            current = to_float(metered_min_i_t[i]);
        } else { // METRIC_AVG
            current = to_float(metered_avg_i_t[i]);
        }

        // Calculate exponential decay weight: w(i) = decay^i
        // Recent frames (i=0) have weight=1.0, older frames decay exponentially
        float weight = pow(TEMPORAL_DECAY, float(i));

        // Harmonic mean: H = sum(w) / sum(w/x)
        sum_inv_weighted += weight / max(current, 1e-6);
        sum_weights += weight;
    }

    // Return weighted harmonic mean
    return sum_weights / max(sum_inv_weighted, 1e-6);
}

/**
 * Applies Exponential Moving Average (EMA) smoothing
 * Provides additional stability on top of weighted average
 *
 * @param new_value New computed value
 * @param prev_value Previous frame's value
 * @return Smoothed value: prev + alpha * (new - prev)
 */
float apply_ema_smoothing(float new_value, float prev_value) {
    return prev_value + TEMPORAL_EMA_ALPHA * (new_value - prev_value);
}

/**
 * Gradually transitions temporal buffers during scene changes
 * Blends old values towards new values to avoid sudden jumps
 *
 * @param count Number of frames within the time window
 */
void temporal_fill_gradual(uint count) {
    for (uint i = 0u; i < count; i++) {
        // Blend each buffer entry towards current value
        float old_max = to_float(metered_max_i_t[i]);
        float new_max = to_float(metered_max_i);
        metered_max_i_t[i] = to_uint(mix(old_max, new_max, TEMPORAL_SCENE_BLEND));

        float old_min = to_float(metered_min_i_t[i]);
        float new_min = to_float(metered_min_i);
        metered_min_i_t[i] = to_uint(mix(old_min, new_min, TEMPORAL_SCENE_BLEND));

        float old_avg = to_float(metered_avg_i_t[i]);
        float new_avg = to_float(metered_avg_i);
        metered_avg_i_t[i] = to_uint(mix(old_avg, new_avg, TEMPORAL_SCENE_BLEND));
    }
}

/**
 * Performs linear regression prediction for scene change detection
 * Uses least squares method to predict next frame value
 *
 * @param type Metric type: METRIC_MAX, METRIC_MIN, or METRIC_AVG
 * @param count Number of frames within the time window
 * @return Predicted value for next frame
 */
float temporal_predict(int type, uint count) {
    float sum_x = 0.0;
    float sum_y = 0.0;
    float sum_x2 = 0.0;
    float sum_xy = 0.0;

    float n = float(count);
    float xp = n + 1.0; // Predict position n+1

    // Accumulate sums for least squares regression
    for (uint i = 0u; i < count; i++) {
        float x = float(i + 1u);
        float y;

        // Select appropriate buffer based on metric type
        if (type == METRIC_MAX) {
            y = to_float(metered_max_i_t[i]);
        } else if (type == METRIC_MIN) {
            y = to_float(metered_min_i_t[i]);
        } else {
            y = to_float(metered_avg_i_t[i]);
        }

        sum_x += x;
        sum_y += y;
        sum_x2 += x * x;
        sum_xy += x * y;
    }

    // Calculate linear regression coefficients
    // y = a*x + b
    float denominator = n * sum_x2 - sum_x * sum_x;
    float a = (n * sum_xy - sum_x * sum_y) / denominator;
    float b = (sum_y - a * sum_x) / n;

    // Return prediction for next frame
    return a * xp + b;
}

/**
 * Detects scene changes using multi-metric prediction error analysis
 * Compares current frame raw values against predictions from history
 *
 * @param max_current Current frame maximum value (raw)
 * @param min_current Current frame minimum value (raw)
 * @param avg_current Current frame average value (raw)
 * @param max_pred Predicted maximum value
 * @param min_pred Predicted minimum value
 * @param avg_pred Predicted average value
 * @return true if scene change is detected
 */
bool is_scene_changed(float max_current, float min_current, float avg_current,
                      float max_pred, float min_pred, float avg_pred) {
    // Detect pure black scenes (always considered a scene change)
    if (max_current < TEMPORAL_BLACK_THRESHOLD) {
        return true;
    }

    // Calculate adaptive tolerance based on current brightness
    // Brighter scenes get higher tolerance to reduce false positives
    float adaptive_tolerance = TEMPORAL_BASE_TOLERANCE *
                               (1.0 + max_current * TEMPORAL_ADAPTIVE_SCALE);

    // Calculate prediction errors in perceptual units (ΔE-like)
    // Compare current values against predictions
    float max_delta = TEMPORAL_DELTA_SCALE * abs(max_current - max_pred);
    float min_delta = TEMPORAL_DELTA_SCALE * abs(min_current - min_pred);
    float avg_delta = TEMPORAL_DELTA_SCALE * abs(avg_current - avg_pred);

    // Combine errors using weighted average
    // Average is most reliable, max is important, min is least reliable
    float weighted_delta = avg_delta * TEMPORAL_WEIGHT_AVG +
                           max_delta * TEMPORAL_WEIGHT_MAX +
                           min_delta * TEMPORAL_WEIGHT_MIN;

    // Scene change detected if weighted error exceeds adaptive threshold
    return weighted_delta > adaptive_tolerance;
}

/**
 * Main temporal stabilization hook
 * Processes max, min, and avg metrics with multi-stage smoothing
 * and intelligent scene change detection
 * Uses PTS-based time window instead of fixed frame count
 */
void hook() {
    // Determine how many history frames are within the time window
    uint count = temporal_frame_count();

    // Cache current frame raw values before any processing
    float max_current = to_float(metered_max_i);
    float min_current = to_float(metered_min_i);
    float avg_current = to_float(metered_avg_i);

    // Get previous frame's smoothed values from persistent buffer
    float prev_max = to_float(smoothed_max_i);
    float prev_min = to_float(smoothed_min_i);
    float prev_avg = to_float(smoothed_avg_i);

    // Generate predictions BEFORE prepending current frame to history
    // This ensures predictions are based on historical data only
    float max_pred = temporal_predict(METRIC_MAX, count);
    float min_pred = temporal_predict(METRIC_MIN, count);
    float avg_pred = temporal_predict(METRIC_AVG, count);

    // Detect scene changes by comparing current raw values against predictions
    bool scene_changed = is_scene_changed(max_current, min_current, avg_current,
                                          max_pred, min_pred, avg_pred);

    // Update temporal history with current frame
    temporal_prepend();

    // Recalculate count after prepend (current frame is now in buffer)
    count = temporal_frame_count();

    // Stage 1: Weighted harmonic mean (exponential decay)
    // Gives more weight to recent frames, resistant to outliers
    float max_weighted = temporal_weighted_mean(METRIC_MAX, count);
    float min_weighted = temporal_weighted_mean(METRIC_MIN, count);
    float avg_weighted = temporal_weighted_mean(METRIC_AVG, count);

    // Stage 2: Exponential moving average smoothing
    // Uses previous frame's smoothed output for proper EMA
    float max_smoothed = apply_ema_smoothing(max_weighted, prev_max);
    float min_smoothed = apply_ema_smoothing(min_weighted, prev_min);
    float avg_smoothed = apply_ema_smoothing(avg_weighted, prev_avg);

    // Handle scene changes
    if (scene_changed) {
        // Gradually transition buffer values to new scene
        temporal_fill_gradual(count);

        // Apply reduced smoothing for scene cuts to maintain some stability
        // while still responding to the new scene quickly
        max_smoothed = mix(prev_max, max_current, TEMPORAL_CUT_BLEND);
        min_smoothed = mix(prev_min, min_current, TEMPORAL_CUT_BLEND);
        avg_smoothed = mix(prev_avg, avg_current, TEMPORAL_CUT_BLEND);
    }

    // Write back smoothed values
    metered_max_i = to_uint(max_smoothed);
    metered_min_i = to_uint(min_smoothed);
    metered_avg_i = to_uint(avg_smoothed);

    // Store smoothed values for next frame's EMA calculation
    smoothed_max_i = metered_max_i;
    smoothed_min_i = metered_min_i;
    smoothed_avg_i = metered_avg_i;
}

//!HOOK OUTPUT
//!BIND METERED
//!BIND METADATA
//!SAVE EMPTY
//!WIDTH 1
//!HEIGHT 1
//!COMPUTE 1 1
//!DESC metering (metadata)

// For content with dynamic metadata, it will be provided by mpv
// https://github.com/mpv-player/mpv/pull/15239

const float m1 = 2610.0 / 4096.0 / 4.0;
const float m2 = 2523.0 / 4096.0 * 128.0;
const float c1 = 3424.0 / 4096.0;
const float c2 = 2413.0 / 4096.0 * 32.0;
const float c3 = 2392.0 / 4096.0 * 32.0;
const float pw = 10000.0;

float pq_eotf_inv(float x) {
    float t = pow(x / pw, m1);
    return pow((c1 + c2 * t) / (1.0 + c3 * t), m2);
}

float pq_eotf(float x) {
    float t = pow(x, 1.0 / m2);
    return pow(max(t - c1, 0.0) / (c2 - c3 * t), 1.0 / m1) * pw;
}

const float m2_z = 1.7 * m2;

float iz_eotf_inv(float x) {
    float t = pow(x / pw, m1);
    return pow((c1 + c2 * t) / (1.0 + c3 * t), m2_z);
}

float iz_eotf(float x) {
    float t = pow(x, 1.0 / m2_z);
    return pow(max(t - c1, 0.0) / (c2 - c3 * t), 1.0 / m1) * pw;
}

const float d = -0.56;
const float d0 = 1.6295499532821566e-11;

float I_to_J(float I) {
    return ((1.0 + d) * I) / (1.0 + (d * I)) - d0;
}

float J_to_I(float J) {
    return (J + d0) / (1.0 + d - d * (J + d0));
}

float RGB_to_Y(vec3 rgb) {
    const vec3 coefficients = vec3(0.2627002120112671, 0.6779980715188708, 0.05930171646986196);
    return dot(rgb, coefficients);
}

float to_float(uint x) {
    return float(x) / 4095.0;
}

float get_max_i() {
    if (max_pq_y > 0.0)
        return max_pq_y;

    vec3 scene_max_rgb = vec3(scene_max_r, scene_max_g, scene_max_b);
    if (max(max(scene_max_rgb.r, scene_max_rgb.g), scene_max_rgb.b) > 0.0)
        return pq_eotf_inv(RGB_to_Y(scene_max_rgb));

    if (enable_metering > 0)
        return to_float(metered_max_i);

    if (max_cll > 0.0)
        return pq_eotf_inv(max_cll);

    if (max_luma > 0.0)
        return pq_eotf_inv(max_luma);

    return pq_eotf_inv(1000.0);
}

float get_min_i() {
    if (enable_metering > 0)
        return to_float(metered_min_i);

    if (min_luma > 0.0)
        return pq_eotf_inv(min_luma);

    return pq_eotf_inv(0.0);
}

float get_avg_i() {
    if (avg_pq_y > 0.0)
        return avg_pq_y;

    if (scene_avg > 0.0)
        return pq_eotf_inv(scene_avg);

    if (enable_metering > 1)
        return to_float(metered_avg_i);

    // not useful
    // if (max_fall > 0.0)
    //     return pq_eotf_inv(max_fall);

    return 0.0;
}

float get_ev(float avg_i, float max_i, float min_i) {
    float reference_iz = iz_eotf_inv(reference_white);
    float reference_j = I_to_J(reference_iz);
    float anchor_j = auto_exposure_anchor * reference_j;
    float anchor_iz = J_to_I(anchor_j);
    float anchor = iz_eotf(anchor_iz);

    float average = max(pq_eotf(avg_i), 1e-6);
    float maximum = max(pq_eotf(max_i), 1e-6);
    float minimum = max(pq_eotf(min_i), 1e-6);

    float ev = log2(anchor / average);

    float ev_limit_neg = min(auto_exposure_limit_negtive, log2(maximum / average));
    float ev_limit_pos = min(auto_exposure_limit_postive, log2(average / minimum));

    return clamp(ev, -ev_limit_neg, ev_limit_pos);
}

void hook() {
    max_i = get_max_i();
    min_i = get_min_i();
    avg_i = get_avg_i();

    if (avg_i > 0.0 && auto_exposure_anchor > 0.0) {
        ev = get_ev(avg_i, max_i, min_i);
    } else {
        ev = 0.0;
    }

    if (ev != 0.0) {
        float ev_scale = exp2(ev);
        max_i = pq_eotf_inv(pq_eotf(max_i) * ev_scale);
        min_i = pq_eotf_inv(pq_eotf(min_i) * ev_scale);
    }
}

//!HOOK OUTPUT
//!BIND HOOKED
//!BIND METERING
//!BIND METERED
//!WHEN preview_metering
//!DESC metering (metadata highlight)

const float JND = 1.0 / 720.0;

const vec3 red   = vec3(1.0, 0.0, 0.0);
const vec3 green = vec3(0.0, 1.0, 0.0);
const vec3 blue  = vec3(0.0, 0.0, 1.0);

float to_float(uint x) {
    return float(x) / 4095.0;
}

float delta(float a, float b) {
    return abs(a - b);
}

bool approx(float a, float b, float epsilon) {
    return delta(a, b) < epsilon;
}

vec4 draw_highlight(float metric, vec3 tint, float value) {
    if (approx(value, metric, 5.0 * JND))
        return vec4(tint, 0.75);
    return vec4(0.0);
}

vec4 draw_line(float metric, vec3 tint) {
    if (approx(1.0 - METERING_pos.y, metric, fwidth(METERING_pos.y)))
        return vec4(tint, 1.0);
    return vec4(0.0);
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);
    float value = METERING_tex(METERING_pos).x;

    vec4 r = vec4(0.0);

    // highlight pixels with similar intensity
    r = max(r, draw_highlight(to_float(metered_max_i), red, value));
    r = max(r, draw_highlight(to_float(metered_min_i), blue, value));
    if (enable_metering > 1)
        r = max(r, draw_highlight(to_float(metered_avg_i), green, value));

    // draw lines
    // r = max(r, draw_line(to_float(metered_max_i), red));
    // r = max(r, draw_line(to_float(metered_min_i), blue));
    // if (enable_metering > 1)
    //     r = max(r, draw_line(to_float(metered_avg_i), green));

    color.rgb = mix(color.rgb, r.rgb, r.a);
    return color;
}

//!HOOK OUTPUT
//!BIND HOOKED
//!BIND METADATA
//!WHEN preview_metering
//!DESC metering (metadata overlay)

const float m1 = 2610.0 / 4096.0 / 4.0;
const float m2 = 2523.0 / 4096.0 * 128.0;
const float c1 = 3424.0 / 4096.0;
const float c2 = 2413.0 / 4096.0 * 32.0;
const float c3 = 2392.0 / 4096.0 * 32.0;
const float pw = 10000.0;

float pq_eotf(float x) {
    float t = pow(x, 1.0 / m2);
    return pow(max(t - c1, 0.0) / (c2 - c3 * t), 1.0 / m1) * pw;
}

// 3x5 bitmap font rendering
const float CHAR_W = 3.0;
const float CHAR_H = 5.0;
const float SPACING = 1.0;
const float MARGIN = 8.0;
const float PAD = 2.0;
const float SCALE = 4.0;
const float LINE_H = CHAR_H + 2.0;

const uint FONT_0 = 0x7B6Fu;
const uint FONT_1 = 0x749Au;
const uint FONT_2 = 0x73E7u;
const uint FONT_3 = 0x79E7u;
const uint FONT_4 = 0x49EDu;
const uint FONT_5 = 0x79CFu;
const uint FONT_6 = 0x7BCFu;
const uint FONT_7 = 0x4927u;
const uint FONT_8 = 0x7BEFu;
const uint FONT_9 = 0x79EFu;
const uint FONT_A = 0x5BEFu;
const uint FONT_B = 0x3AEBu;
const uint FONT_C = 0x724Fu;
const uint FONT_D = 0x3B6Bu;
const uint FONT_E = 0x72CFu;
const uint FONT_F = 0x12CFu;
const uint FONT_G = 0x7B4Fu;
const uint FONT_H = 0x5BEDu;
const uint FONT_I = 0x7497u;
const uint FONT_J = 0x7B24u;
const uint FONT_K = 0x5AEDu;
const uint FONT_L = 0x7249u;
const uint FONT_M = 0x5BFDu;
const uint FONT_N = 0x5B6Fu;
const uint FONT_O = 0x7B6Fu;
const uint FONT_P = 0x13EFu;
const uint FONT_Q = 0x49EFu;
const uint FONT_R = 0x5AEFu;
const uint FONT_S = 0x388Eu;
const uint FONT_T = 0x2497u;
const uint FONT_U = 0x7B6Du;
const uint FONT_V = 0x256Du;
const uint FONT_W = 0x5FEDu;
const uint FONT_X = 0x5AADu;
const uint FONT_Y = 0x24ADu;
const uint FONT_Z = 0x72A7u;
const uint FONT_COLON = 0x0410u;
const uint FONT_DOT   = 0x2000u;
const uint FONT_MINUS = 0x01C0u;
const uint FONT_SPACE = 0x0000u;
const uint FONT_TOFU  = 0x7FFFu;

const int CH_SPACE = 32;
const int CH_MINUS = 45;
const int CH_DOT   = 46;
const int CH_0 = 48; const int CH_1 = 49; const int CH_2 = 50; const int CH_3 = 51; const int CH_4 = 52;
const int CH_5 = 53; const int CH_6 = 54; const int CH_7 = 55; const int CH_8 = 56; const int CH_9 = 57;
const int CH_COLON = 58;
const int CH_A = 65; const int CH_B = 66; const int CH_C = 67; const int CH_D = 68; const int CH_E = 69;
const int CH_F = 70; const int CH_G = 71; const int CH_H = 72; const int CH_I = 73; const int CH_J = 74;
const int CH_K = 75; const int CH_L = 76; const int CH_M = 77; const int CH_N = 78; const int CH_O = 79;
const int CH_P = 80; const int CH_Q = 81; const int CH_R = 82; const int CH_S = 83; const int CH_T = 84;
const int CH_U = 85; const int CH_V = 86; const int CH_W = 87; const int CH_X = 88; const int CH_Y = 89;
const int CH_Z = 90;

uint get_glyph(int ch) {
    if (ch == CH_SPACE) return FONT_SPACE;
    if (ch == CH_MINUS) return FONT_MINUS;
    if (ch == CH_DOT)   return FONT_DOT;
    if (ch == CH_COLON) return FONT_COLON;
    if (ch == CH_0) return FONT_0;
    if (ch == CH_1) return FONT_1;
    if (ch == CH_2) return FONT_2;
    if (ch == CH_3) return FONT_3;
    if (ch == CH_4) return FONT_4;
    if (ch == CH_5) return FONT_5;
    if (ch == CH_6) return FONT_6;
    if (ch == CH_7) return FONT_7;
    if (ch == CH_8) return FONT_8;
    if (ch == CH_9) return FONT_9;
    if (ch == CH_A) return FONT_A;
    if (ch == CH_B) return FONT_B;
    if (ch == CH_C) return FONT_C;
    if (ch == CH_D) return FONT_D;
    if (ch == CH_E) return FONT_E;
    if (ch == CH_F) return FONT_F;
    if (ch == CH_G) return FONT_G;
    if (ch == CH_H) return FONT_H;
    if (ch == CH_I) return FONT_I;
    if (ch == CH_J) return FONT_J;
    if (ch == CH_K) return FONT_K;
    if (ch == CH_L) return FONT_L;
    if (ch == CH_M) return FONT_M;
    if (ch == CH_N) return FONT_N;
    if (ch == CH_O) return FONT_O;
    if (ch == CH_P) return FONT_P;
    if (ch == CH_Q) return FONT_Q;
    if (ch == CH_R) return FONT_R;
    if (ch == CH_S) return FONT_S;
    if (ch == CH_T) return FONT_T;
    if (ch == CH_U) return FONT_U;
    if (ch == CH_V) return FONT_V;
    if (ch == CH_W) return FONT_W;
    if (ch == CH_X) return FONT_X;
    if (ch == CH_Y) return FONT_Y;
    if (ch == CH_Z) return FONT_Z;
    return FONT_TOFU;
}

bool glyph_pixel(uint glyph, vec2 p) {
    if (p.x < 0.0 || p.x >= CHAR_W || p.y < 0.0 || p.y >= CHAR_H) return false;
    uint bit = uint(p.y) * 3u + uint(p.x);
    return (glyph & (1u << bit)) != 0u;
}

uint extract_digit(uint value, uint pos) {
    uint d = 1u;
    for (uint i = 0u; i < pos; i++) d *= 10u;
    return (value / d) % 10u;
}

vec4 draw_background(vec2 origin, vec2 px, float width) {
    vec2 local = (px - origin) / SCALE;
    if (local.x >= -PAD && local.x <= width + PAD &&
        local.y >= -PAD && local.y <= CHAR_H + PAD)
        return vec4(0.0, 0.0, 0.0, 0.7);
    return vec4(0.0);
}

vec4 draw_char(int ch, vec2 origin, vec2 px, inout float cx) {
    vec2 local = (px - origin) / SCALE;
    vec2 cp = local - vec2(cx, 0.0);
    cx += CHAR_W + SPACING;
    if (cp.x >= 0.0 && cp.x < CHAR_W && cp.y >= 0.0 && cp.y < CHAR_H) {
        if (glyph_pixel(get_glyph(ch), cp))
            return vec4(1.0, 1.0, 1.0, 1.0);
    }
    return vec4(0.0);
}

vec4 draw_number(float value, vec2 origin, vec2 px, inout float cx) {
    bool negative = value < 0.0;
    float abs_val = min(abs(value), 99999.99);

    uint int_part = uint(abs_val);
    uint dec_part = uint(fract(abs_val) * 100.0 + 0.5);
    if (dec_part >= 100u) {
        int_part += 1u;
        dec_part -= 100u;
    }

    uint d0 = extract_digit(int_part, 4u);
    uint d1 = extract_digit(int_part, 3u);
    uint d2 = extract_digit(int_part, 2u);
    uint d3 = extract_digit(int_part, 1u);
    uint d4 = extract_digit(int_part, 0u);
    uint d5 = extract_digit(dec_part, 1u);
    uint d6 = extract_digit(dec_part, 0u);

    uint first = 4u;
    if (d0 > 0u) first = 0u;
    else if (d1 > 0u) first = 1u;
    else if (d2 > 0u) first = 2u;
    else if (d3 > 0u) first = 3u;

    vec4 r = vec4(0.0);

    if (negative)    r = max(r, draw_char(CH_MINUS, origin, px, cx));
    if (first <= 0u) r = max(r, draw_char(int(d0) + CH_0, origin, px, cx));
    if (first <= 1u) r = max(r, draw_char(int(d1) + CH_0, origin, px, cx));
    if (first <= 2u) r = max(r, draw_char(int(d2) + CH_0, origin, px, cx));
    if (first <= 3u) r = max(r, draw_char(int(d3) + CH_0, origin, px, cx));
    r = max(r, draw_char(int(d4) + CH_0, origin, px, cx));
    r = max(r, draw_char(CH_DOT, origin, px, cx));
    r = max(r, draw_char(int(d5) + CH_0, origin, px, cx));
    r = max(r, draw_char(int(d6) + CH_0, origin, px, cx));

    return r;
}

// Draw a labeled row: "LABEL:value"
// Returns max cx across all rows for background width.
vec4 draw_row(float value, vec2 origin, vec2 px, int c0, int c1, int c2, inout float cx) {
    vec4 r = vec4(0.0);
    r = max(r, draw_char(c0, origin, px, cx));
    r = max(r, draw_char(c1, origin, px, cx));
    r = max(r, draw_char(c2, origin, px, cx));
    r = max(r, draw_char(CH_COLON, origin, px, cx));
    r = max(r, draw_number(value, origin, px, cx));
    return r;
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);
    vec2 px = HOOKED_pos * HOOKED_size;

    float max_nits = pq_eotf(max_i);
    float min_nits = pq_eotf(min_i);
    float avg_nits = pq_eotf(avg_i);

    vec4 r = vec4(0.0);
    float max_w = 0.0;

    // Row 0 (bottom): "EV :value"
    vec2 o3 = vec2(MARGIN * SCALE, HOOKED_size.y - MARGIN * SCALE - CHAR_H * SCALE);
    float cx3 = 0.0;

    // Row 1: "AVG:value"
    vec2 o2 = o3 - vec2(0.0, LINE_H * SCALE);
    float cx2 = 0.0;
    r = max(r, draw_row(avg_nits, o2, px, CH_A, CH_V, CH_G, cx2));
    max_w = max(max_w, cx2);

    // Row 2: "MIN:value"
    vec2 o1 = o2 - vec2(0.0, LINE_H * SCALE);
    float cx1 = 0.0;
    r = max(r, draw_row(min_nits, o1, px, CH_M, CH_I, CH_N, cx1));
    max_w = max(max_w, cx1);

    // Row 3 (top): "MAX:value"
    vec2 o0 = o1 - vec2(0.0, LINE_H * SCALE);
    float cx0 = 0.0;
    r = max(r, draw_row(max_nits, o0, px, CH_M, CH_A, CH_X, cx0));
    max_w = max(max_w, cx0);

    r = max(r, draw_row(ev, o3, px, CH_E, CH_V, CH_SPACE, cx3));
    max_w = max(max_w, cx3);

    // Background: cover all 4 rows (o0 is top, o3 is bottom)
    if (r.a == 0.0) {
        vec2 local = (px - o0) / SCALE;
        float total_h = 3.0 * LINE_H + CHAR_H;
        if (local.x >= -PAD && local.x <= max_w + PAD &&
            local.y >= -PAD && local.y <= total_h + PAD)
            r = vec4(0.0, 0.0, 0.0, 1.0);
    }

    color.rgb = mix(color.rgb, r.rgb, r.a);
    return color;
}

//!HOOK OUTPUT
//!BIND HOOKED
//!BIND METADATA
//!WHEN auto_exposure_anchor 0 > enable_metering 1 > avg_pq_y 0 > + scene_avg 0 > + *
//!DESC tone mapping (auto exposure)

vec3 exposure(vec3 x, float ev) {
    return x * exp2(ev);
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = exposure(color.rgb, ev);

    return color;
}

//!HOOK OUTPUT
//!BIND HOOKED
//!BIND METADATA
//!DESC tone mapping (astra)

const float m1 = 2610.0 / 4096.0 / 4.0;
const float m2 = 2523.0 / 4096.0 * 128.0;
const float c1 = 3424.0 / 4096.0;
const float c2 = 2413.0 / 4096.0 * 32.0;
const float c3 = 2392.0 / 4096.0 * 32.0;
const float pw = 10000.0;

float pq_eotf_inv(float x) {
    float t = pow(x / pw, m1);
    return pow((c1 + c2 * t) / (1.0 + c3 * t), m2);
}

vec3 pq_eotf_inv(vec3 x) {
    vec3 t = pow(x / pw, vec3(m1));
    return pow((c1 + c2 * t) / (1.0 + c3 * t), vec3(m2));
}

float pq_eotf(float x) {
    float t = pow(x, 1.0 / m2);
    return pow(max(t - c1, 0.0) / (c2 - c3 * t), 1.0 / m1) * pw;
}

vec3 pq_eotf(vec3 x) {
    vec3 t = pow(x, vec3(1.0 / m2));
    return pow(max(t - c1, 0.0) / (c2 - c3 * t), vec3(1.0 / m1)) * pw;
}

// Jzazbz added a factor to m2, which differs from the original PQ equation.
const float m2_z = 1.7 * m2;

float iz_eotf_inv(float x) {
    float t = pow(x / pw, m1);
    return pow((c1 + c2 * t) / (1.0 + c3 * t), m2_z);
}

vec3 iz_eotf_inv(vec3 x) {
    vec3 t = pow(x / pw, vec3(m1));
    return pow((c1 + c2 * t) / (1.0 + c3 * t), vec3(m2_z));
}

float iz_eotf(float x) {
    float t = pow(x, 1.0 / m2_z);
    return pow(max(t - c1, 0.0) / (c2 - c3 * t), 1.0 / m1) * pw;
}

vec3 iz_eotf(vec3 x) {
    vec3 t = pow(x, vec3(1.0 / m2_z));
    return pow(max(t - c1, 0.0) / (c2 - c3 * t), vec3(1.0 / m1)) * pw;
}

vec3 RGB_to_XYZ(vec3 RGB) {
    const mat3 M = mat3(
        0.6369580483012914, 0.14461690358620832,  0.1688809751641721,
        0.2627002120112671, 0.6779980715188708,   0.05930171646986196,
        0.0               , 0.028072693049087428, 1.060985057710791
    );
    return RGB * M;
}

vec3 XYZ_to_RGB(vec3 XYZ) {
    const mat3 M = mat3(
         1.716651187971268, -0.355670783776392, -0.25336628137366,
        -0.666684351832489,  1.616481236634939,  0.0157685458139111,
         0.017639857445311, -0.042770613257809,  0.942103121235474
    );
    return XYZ * M;
}

const float b = 1.15;
const float g = 0.66;

vec3 XYZ_to_XYZm(vec3 XYZ) {
    float Xm = (b * XYZ.x) - ((b - 1.0) * XYZ.z);
    float Ym = (g * XYZ.y) - ((g - 1.0) * XYZ.x);
    return vec3(Xm, Ym, XYZ.z);
}

vec3 XYZm_to_XYZ(vec3 XYZm) {
    float Xa = (XYZm.x + ((b - 1.0) * XYZm.z)) / b;
    float Ya = (XYZm.y + ((g - 1.0) * Xa)) / g;
    return vec3(Xa, Ya, XYZm.z);
}

vec3 XYZ_to_LMS(vec3 XYZ) {
    const mat3 M =mat3(
         0.41478972, 0.579999, 0.0146480,
        -0.2015100,  1.120649, 0.0531008,
        -0.0166008,  0.264800, 0.6684799
    );
    return XYZ * M;
}

vec3 LMS_to_XYZ(vec3 LMS) {
    const mat3 M = mat3(
         1.9242264357876067,  -1.0047923125953657,  0.037651404030618,
         0.35031676209499907,  0.7264811939316552, -0.06538442294808501,
        -0.09098281098284752, -0.3127282905230739,  1.5227665613052603
    );
    return LMS * M;
}

vec3 LMS_to_Iab(vec3 LMS) {
    const mat3 M = mat3(
        0.0,       0.5,       0.5,
        3.524000, -4.066708,  0.542708,
        0.199076,  1.096799, -1.295875
    );
    return LMS * M;
}

vec3 Iab_to_LMS(vec3 Iab) {
    const mat3 M = mat3(
        1.0,  0.13860504327153927,  0.05804731615611883,
        1.0, -0.1386050432715393,  -0.058047316156118904,
        1.0, -0.09601924202631895, -0.81189189605603900
    );
    return Iab * M;
}

// https://doi.org/10.2352/ISSN.2169-2629.2017.25.264
// Optimized matrices for Jzazbz about LMS to I conversion.
// https://doi.org/10.1364/OE.413659
// ZCAM defines Iz = G' - ε, where ε = 3.7035226210190005e-11.
// However, it appears we do not need it.
vec3 LMS_to_Iab_optimized(vec3 LMS) {
    const mat3 M = mat3(
        0.0,       1.0,       0.0,
        3.524000, -4.066708,  0.542708,
        0.199076,  1.096799, -1.295875
    );
    return LMS * M;
}

vec3 Iab_to_LMS_optimized(vec3 Iab) {
    const mat3 M = mat3(
        1.0, 0.2772100865430786,  0.1160946323122377,
        1.0, 0.0,                 0.0,
        1.0, 0.0425858012452203, -0.75384457989992
    );
    return Iab * M;
}

const float d = -0.56;
const float d0 = 1.6295499532821566e-11;

float I_to_J(float I) {
    return ((1.0 + d) * I) / (1.0 + (d * I)) - d0;
}

float J_to_I(float J) {
    return (J + d0) / (1.0 + d - d * (J + d0));
}

// CIELUV: -0.01585, -0.03017, -0.04556, -0.02667, -0.00295, 0.14592, 0.05084, -0.01900, -0.00764
float hke_fh_nayatani(
    float h, float k1,
    float k2, float k3, float k4, float k5,
    float k6, float k7, float k8, float k9
) {
    float q = k1 +
        k2 * cos(h) + k3 * cos(2.0 * h) + k4 * cos(3.0 * h) + k5 * cos(4.0 * h) +
        k6 * sin(h) + k7 * sin(2.0 * h) + k8 * sin(3.0 * h) + k9 * sin(4.0 * h);
    // flipped
    return -q;
}

// CIECAM02: -0.218, 0.167, -0.500, 0.032, 0.887
// CAM16: -0.160, 0.132, -0.405, 0.080, 0.792
float hke_fh_hellwig(float h, float a1, float a2, float a3, float a4, float a5) {
    return a1 * cos(h) + a2 * cos(2.0 * h) + a3 * sin(h) + a4 * sin(2.0 * h) + a5;
}

// CIELAB: 0.1644, 0.0603, 0.1307, 0.0060
float hke_fh_high(float h, float k1, float k2, float k3, float k4) {
    h = mod(mod(degrees(h), 360.0) + 360.0, 360.0);
    float by = k1 * abs(sin(radians((h - 90.0)/ 2.0))) + k2;
    float r  = h <= 90.0 || h >= 270.0 ? k3 * abs(cos(radians(h))) + k4 : 0.0;
    return by + r;
}

// CIECAM16: 1.5940, 45.0, 2.6518
// CIELAB: 0.1644, 45.0, 0.1024
float hke_fh_liao(float h, float k3, float k4, float k5) {
    h = mod(mod(degrees(h), 360.0) + 360.0, 360.0);
    return k3 * abs(log(((h + k4) / (90.0 + k4)))) + k5;
}

float hke_fh(float h) {
    float result = hke_fh_liao(h, 0.1351, 45.0, 0.1439);
    return result * hk_effect_compensate_scaling;
}

// Lightness modifications of the CIECAM16 and CIELAB based
// on the Helmholtz-Kohlrausch effect
// by Liao et al.
// https://doi.org/10.1364/OE.534073
float J_to_Jhk(vec3 JCh) {
    float J = JCh.x;
    float C = JCh.y;
    float h = JCh.z;
    return J + C * hke_fh(h);
}

float Jhk_to_J(vec3 JCh) {
    float J = JCh.x;
    float C = JCh.y;
    float h = JCh.z;
    return J - C * hke_fh(h);
}

// https://www.itu.int/rec/R-REC-BT.2124
// ΔE_ITP_JND = 1 / 720
// 0.0001 of Cz is much smaller than it
const float epsilon = 0.0001;

vec3 Lab_to_LCh(vec3 Lab) {
    float L = Lab.x;
    float a = Lab.y;
    float b = Lab.z;

    float C = length(vec2(a, b));
    float h = C < epsilon ? 0.0 : atan(b, a);

    return vec3(L, C, h);
}

vec3 LCh_to_Lab(vec3 LCh) {
    float L = LCh.x;
    float C = LCh.y;
    float h = LCh.z;

    C = max(C, 0.0);
    float a = C * cos(h);
    float b = C * sin(h);

    return vec3(L, a, b);
}

// Perceptually uniform color space for image signals including
// high dynamic range and wide gamut
// by Safdar et al.
// https://doi.org/10.1364/OE.25.015131
//
// an optimized version of the LMS to Iab matrix was used,
// and H-K effect compensation was added.
vec3 RGB_to_Jab(vec3 color) {
    color *= reference_white;
    color = RGB_to_XYZ(color);
    color = XYZ_to_XYZm(color);
    color = XYZ_to_LMS(color);
    color = iz_eotf_inv(color);
    color = LMS_to_Iab_optimized(color);
    color.x = I_to_J(color.x);
    color.x = J_to_Jhk(Lab_to_LCh(color));
    return color;
}

vec3 Jab_to_RGB(vec3 color) {
    color.x = Jhk_to_J(Lab_to_LCh(color));
    color.x = J_to_I(color.x);
    color = Iab_to_LMS_optimized(color);
    color = iz_eotf(color);
    color = LMS_to_XYZ(color);
    color = XYZm_to_XYZ(color);
    color = XYZ_to_RGB(color);
    color /= reference_white;
    return color;
}

float f_slope(float x0, float y0, float x1, float y1) {
    float num = (y1 - y0);
    float den = (x1 - x0);
    return abs(den) < 1e-6 ? 1.0 : num / den;
}

float f_intercept(float slope, float x0, float y0) {
    return y0 - slope * x0;
}

float f_linear(float x, float slope, float intercept) {
    return slope * x + intercept;
}

// Linear relationship between angle and parameter c
// c = 0: angle = 45° (slope = 1)
// c = ±N: angle = 45° ± k*N
float f_contrast(float c) {
    float range = 40.0; // 40° per unit of c
    float angle = radians(45.0 + range * c);
    float slope = tan(angle);
    return 1.0 - 1.0 / slope;
}

// Hyperbola tone mapping
// by suzuki et al.
// https://technorgb.blogspot.com/2018/02/hyperbola-tone-mapping.html
float f_toe_suzuki(float x, float slope, float x0, float y0, float x1, float y1) {
    float dx = x1 - x0;
    float dy = y1 - y0;
    float dt = x - x0;
    float k = dy - slope * dx;
    float scale = dy * dy;
    float base = slope * dx * dx;

    return y0 + scale * dt / (dt * k + base);
}

float f_shoulder_suzuki(float x, float slope, float x0, float y0, float x1, float y1) {
    float dx = x1 - x0;
    float dy = y1 - y0;
    float dt = x - x0;
    float k = slope * dx - dy;
    float scale = slope * dx * dy;
    float base = dx * dy;

    return y0 + scale * dt / (dt * k + base);
}

// Filmic Tonemapping with Piecewise Power Curves
// by John Hable
// http://filmicworlds.com/blog/filmic-tonemapping-with-piecewise-power-curves/
float f_toe_hable(float x, float slope, float x0, float y0, float x1, float y1) {
    float dx = x1 - x0;
    float dy = y1 - y0;

    float b = slope * dx / dy;
    float a = log(dy) - b * log(dx);
    float s = 1.0;

    float v = max((x - x0) * s, 1e-6);
    float o = y0;

    return exp(a + b * log(v)) * s + o;
}

float f_shoulder_hable(float x, float slope, float x0, float y0, float x1, float y1) {
    float dx = x1 - x0;
    float dy = y1 - y0;

    float b = slope * dx / dy;
    float a = log(dy) - b * log(dx);
    float s = -1.0;

    float v = max((x - x1) * s, 1e-6);
    float o = y1;

    return exp(a + b * log(v)) * s + o;
}

// Hable shoulder with overshoot: extends the virtual white point to
// (x1 + overshoot * dx, y1 + overshoot * dy), so the curve still has
// non-zero slope at x1.  Accepts a slight slope discontinuity at x0.
// overshoot = 0 recovers f_shoulder_hable.
float f_shoulder_hable_overshoot(float x, float slope, float x0, float y0, float x1, float y1, float overshoot) {
    float dx = x1 - x0;
    float dy = y1 - y0;
    float vx = x1 + overshoot * dx;
    float vy = y1 + overshoot * dy;

    float y  = f_shoulder_hable(x,  slope, x0, y0, vx, vy);
    float yw = f_shoulder_hable(x1, slope, x0, y0, vx, vy);

    float t = (y - y0) / (yw - y0);

    return mix(y0, y1, t);
}

float f(
    float x, float iw, float ib, float ow, float ob,
    float sw, float hw, float cb
) {
    float midgray   = 0.5 * ow;
    float shadow    = mix(midgray, ob, sw);
    float highlight = mix(midgray, ow, hw);
    float contrast  = f_contrast(cb);

    float x0 = ib;
    float y0 = ob;
    float x1 = mix(shadow, midgray, contrast);
    float y1 = shadow;
    float x2 = mix(highlight, midgray, contrast);
    float y2 = highlight;
    float x3 = iw;
    float y3 = ow;

    float slope = f_slope(x1, y1, x2, y2);
    float intercept = f_intercept(slope, x1, y1);

    if (x >= x1 && x <= x2) {
        return f_linear(x, slope, intercept);
    }

    if (x < x1) {
        float slope_toe = f_slope(x0, y0, x1, y1);
        if (slope_toe >= slope) {
            return f_linear(x, slope, intercept);
        }

        return f_toe_suzuki(x, slope, x0, y0, x1, y1);
    }

    if (x > x2) {
        float slope_shoulder = f_slope(x2, y2, x3, y3);
        if (slope_shoulder >= slope) {
            return f_linear(x, slope, intercept);
        }

        return f_shoulder_hable_overshoot(x, slope, x2, y2, x3, y3, highlight_overshoot);
    }

    return x;
}

float f(float x, float iw, float ib, float ow, float ob) {
    return f(
        x, iw, ib, ow, ob,
        shadow_weight, highlight_weight, contrast_bias
    );
}

float curve(float x) {
    float ow = I_to_J(iz_eotf_inv(reference_white));
    float ob = I_to_J(iz_eotf_inv(reference_white / contrast_ratio));
    float iw = I_to_J(iz_eotf_inv(pq_eotf(max_i)));
    float ib = I_to_J(iz_eotf_inv(pq_eotf(min_i)));

    iw = max(iw, ow);
    ib = min(ib, ob);

    float y = f(x, iw, ib, ow, ob);

    return clamp(y, ob, ow);
}

float chroma_correction_attenuation(float x, float rate, float threshold) {
    float norm = max((x - threshold) / (1.0 - threshold), 0.0);
    return pow(norm, 1.0 + rate * (1.0 - norm));
}

// based on the chroma correction method for ICtCp in BT.2390/BT.2408
// https://www.itu.int/pub/R-REP-BT.2408
//
// a power factor is added to increase correction rate.
//
// this is a correction in generic vividness and depth.
// V = sqrt(J^2 + C^2)
// D = sqrt((J_max - J)^2 + C^2)
//
// more specific definitions of V and D for Jzazbz,
// see the following links:
// https://doi.org/10.2352/ISSN.2169-2629.2018.26.96
// https://doi.org/10.2352/issn.2169-2629.2019.27.43
vec2 chroma_correction(vec2 ab, float l1, float l2) {
    float ratio_min = min(l1, l2) / max(max(l1, l2), 1e-6);
    float ratio_scaled = mix(1.0, ratio_min, chroma_correction_scaling);
    float ratio_safe = max(ratio_scaled, 0.0);
    return ab * chroma_correction_attenuation(
        ratio_safe,
        chroma_correction_rate,
        chroma_correction_threshold
    );
}

vec3 tone_mapping(vec3 lab) {
    float l2 = curve(lab.x);
    vec2 ab2 = chroma_correction(lab.yz, lab.x, l2);
    return vec3(l2, ab2);
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = RGB_to_Jab(color.rgb);
    color.rgb = tone_mapping(color.rgb);
    color.rgb = Jab_to_RGB(color.rgb);

    return color;
}
