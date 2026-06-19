// https://developer.mozilla.org/en-US/docs/Web/CSS/transform

//!PARAM origin_x
//!TYPE float
0.5

//!PARAM origin_y
//!TYPE float
0.5

//!PARAM rotate
//!TYPE float
0.0

//!PARAM translate_x
//!TYPE float
0.0

//!PARAM translate_y
//!TYPE float
0.0

//!PARAM scale_x
//!TYPE float
1.0

//!PARAM scale_y
//!TYPE float
1.0

//!PARAM skew_x
//!TYPE float
0.0

//!PARAM skew_y
//!TYPE float
0.0

//!PARAM background
//!TYPE ENUM int
black
border
repeat
mirror

//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transform

mat3 make_rotate(float deg) {
    float rad = radians(deg);
    float c = cos(rad);
    float s = sin(rad);
    return mat3(
        c, -s, 0.0,
        s, c, 0.0,
        0.0, 0.0, 1.0
    );
}

mat3 make_translate(vec2 offset) {
    return mat3(
        1.0, 0.0, 0.0,
        0.0, 1.0, 0.0,
        -offset.x, -offset.y, 1.0
    );
}

mat3 make_scale(vec2 scale) {
    return mat3(
        1.0 / scale.x, 0.0, 0.0,
        0.0, 1.0 / scale.y, 0.0,
        0.0, 0.0, 1.0
    );
}

mat3 make_skew(vec2 deg) {
    vec2 rad = radians(deg);
    return mat3(
        1.0, -tan(rad.y), 0.0,
        -tan(rad.x), 1.0, 0.0,
        0.0, 0.0, 1.0
    );
}

vec2 apply_transform(vec2 coord, mat3 matrix) {
    vec3 homogeneous = vec3(coord, 1.0);
    vec3 transformed = matrix * homogeneous;
    return transformed.xy / transformed.z;
}

vec4 hook() {
    vec2 pos = HOOKED_pos;
    vec2 size = HOOKED_size;

    pos = apply_transform(
        pos,
        make_translate(-vec2(origin_x, origin_y)) *
        make_scale(size) *
        make_rotate(rotate) *
        make_scale(1.0 / size) *
        make_translate(vec2(translate_x, translate_y)) *
        make_scale(vec2(scale_x, scale_y)) *
        make_skew(vec2(skew_x, skew_y)) *
        make_translate(vec2(origin_x, origin_y))
    );

    bool out_of_bounds = pos.x < 0.0 || pos.x > 1.0 || pos.y < 0.0 || pos.y > 1.0;
    if (out_of_bounds) {
        if (background == black) {
            return vec4(vec3(0.0), 1.0);
        } else if (background == border) {
            pos = clamp(pos, 0.0, 1.0);
        } else if (background == repeat) {
            pos = mod(pos, 1.0);
        } else if (background == mirror) {
            pos = 1.0 - abs(fract(pos * 0.5) * 2.0 - 1.0);
        }
    }

    return HOOKED_tex(pos);
}
