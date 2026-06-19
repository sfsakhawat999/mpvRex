// https://anirban-karchaudhuri.medium.com/edge-detection-methods-comparison-9e4b75a9bf87
// I suppose the results obtained by processing under PQ(Y) are consistent with human perception.

//!PARAM edge_detection
//!TYPE ENUM int
laplacian
sobel
prewitt

//!PARAM printmaking
//!TYPE ENUM int
relief
intaglio

//!HOOK OUTPUT
//!BIND HOOKED
//!DESC edge detection

const mat3 prewitt_x = mat3(
    1.0, 0.0, -1.0,
    1.0, 0.0, -1.0,
    1.0, 0.0, -1.0
);

const mat3 prewitt_y = mat3(
     1.0,  1.0,  1.0,
     0.0,  0.0,  0.0,
    -1.0, -1.0, -1.0
);

const mat3 sobel_x = mat3(
    1.0, 0.0, -1.0,
    2.0, 0.0, -2.0,
    1.0, 0.0, -1.0
);

const mat3 sobel_y = mat3(
     1.0,  2.0,  1.0,
     0.0,  0.0,  0.0,
    -1.0, -2.0, -1.0
);

const mat3 laplacian_p = mat3(
    0.0,  1.0, 0.0,
    1.0, -4.0, 1.0,
    0.0,  1.0, 0.0
);

const mat3 laplacian_n = mat3(
    0.0,  -1.0,  0.0,
    -1.0,  4.0, -1.0,
    0.0,  -1.0,  0.0
);

const float base        = 0.0;
const uvec2 k_size      = uvec2(3, 3);
const vec2  k_size_h    = vec2(k_size / 2);

vec3 conv(mat3 k) {
    vec3 x = vec3(base);
    for (uint i = 0; i < k_size.x; i++)
        for (uint j = 0; j < k_size.y; j++)
            x += HOOKED_texOff(vec2(j, i) - k_size_h).rgb * k[i][j];
    return x;
}

vec3 make_prewitt() {
    vec3 x = conv(prewitt_x);
    vec3 y = conv(prewitt_y);
    vec3 g = abs(sqrt(x * x + y * y));
    return g;
}

vec3 make_sobel() {
    vec3 x = conv(sobel_x);
    vec3 y = conv(sobel_y);
    vec3 g = abs(sqrt(x * x + y * y));
    return g;
}

vec3 make_laplacian() {
    vec3 x = conv(laplacian_p);
    return x;
}

vec4 hook() {
    vec4 color = vec4(vec3(0.5), 1.0);

    float s = (printmaking == relief) ? -1.0 : 1.0;

    if (edge_detection == laplacian) {
        color.rgb += s * make_laplacian();
    } else if (edge_detection == sobel) {
        color.rgb += s * make_sobel();
    } else if (edge_detection == prewitt) {
        color.rgb += s * make_prewitt();
    }

    return color;
}
