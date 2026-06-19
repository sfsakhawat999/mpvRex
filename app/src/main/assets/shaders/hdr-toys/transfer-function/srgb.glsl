// https://github.com/ampas/aces-core/blob/dev/lib/Lib.Academy.DisplayEncoding.ctl
// moncurve with gamma of 2.4 and offset of 0.055 matches the EOTF found in IEC 61966-2-1:1999 (sRGB)

//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (srgb)

const float gamma = 2.4;
const float offset = 0.055;

float monitor_curve_eotf_inv(float y) {
    const float yb = pow(offset * gamma / ((gamma - 1.0) * (1.0 + offset)), gamma);
    const float rs = pow((gamma - 1.0) / offset, gamma - 1.0) * pow((1.0 + offset) / gamma, gamma);
    return y >= yb ? (1.0 + offset) * pow(y, 1.0 / gamma) - offset : y * rs;
}

vec3 monitor_curve_eotf_inv(vec3 color) {
    return vec3(
        monitor_curve_eotf_inv(color.r),
        monitor_curve_eotf_inv(color.g),
        monitor_curve_eotf_inv(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = monitor_curve_eotf_inv(color.rgb);

    return color;
}
