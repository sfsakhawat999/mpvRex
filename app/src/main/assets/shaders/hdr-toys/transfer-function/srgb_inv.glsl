// https://github.com/ampas/aces-core/blob/dev/lib/Lib.Academy.DisplayEncoding.ctl
// moncurve with gamma of 2.4 and offset of 0.055 matches the EOTF found in IEC 61966-2-1:1999 (sRGB)

//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (srgb, inverse)

const float gamma = 2.4;
const float offset = 0.055;

float monitor_curve_eotf(float x) {
    const float fs = ((gamma - 1.0) / offset) * pow(offset * gamma / ((gamma - 1.0) * (1.0 + offset)), gamma);
    const float xb = offset / (gamma - 1.0);
    return x >= xb ? pow((x + offset) / (1.0 + offset), gamma) : x * fs;
}

vec3 monitor_curve_eotf(vec3 color) {
    return vec3(
        monitor_curve_eotf(color.r),
        monitor_curve_eotf(color.g),
        monitor_curve_eotf(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = monitor_curve_eotf(color.rgb);

    return color;
}
