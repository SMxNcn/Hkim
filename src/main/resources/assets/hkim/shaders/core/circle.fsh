#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in vec4 vertexColor;
in vec2 fragCoord;

out vec4 fragColor;

void main() {
    // Shape parameters packed in TextureMat:
    // TextureMat[0] = (cx, cy, pad, pad)
    // TextureMat[1] = (radius, borderWidth, borderR, borderG)
    // TextureMat[2] = (borderB, borderA, pad, pad)
    vec2 center = TextureMat[0].xy;
    float radius = TextureMat[1][0];

    float dist = length(fragCoord - center);

    float outerAlpha = 1.0 - smoothstep(radius - 0.25, radius + 0.25, dist);

    vec4 fillColor = vertexColor;
    float borderWidth = TextureMat[1][1];

    if (borderWidth > 0.0) {
        float innerR = max(radius - borderWidth, 0.0);

        float innerAlpha = 1.0 - smoothstep(innerR - 0.25, innerR + 0.25, dist);
        float borderMask = outerAlpha - innerAlpha;

        vec4 borderColor = vec4(TextureMat[1][2], TextureMat[1][3], TextureMat[2][0], TextureMat[2][1]);

        float fillA = fillColor.a * innerAlpha;
        vec3 fillPm = fillColor.rgb * fillA;

        float borderA = borderColor.a * borderMask;
        vec3 borderPm = borderColor.rgb * borderA;

        float outA = borderA + fillA * (1.0 - borderA);
        vec3 outPm = borderPm + fillPm * (1.0 - borderA);

        vec3 outRgb = (outA > 0.001) ? outPm / outA : vec3(0.0);

        if (outA < 0.01) discard;
        fragColor = vec4(outRgb, outA);
    } else {
        if (outerAlpha < 0.01) discard;
        fragColor = vec4(fillColor.rgb, fillColor.a * outerAlpha);
    }
}
