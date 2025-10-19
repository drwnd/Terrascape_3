#version 400 core

uniform sampler2D depthTexture;
uniform sampler2D noiseTexture;

uniform mat4 projectionMatrix;
uniform mat4 projectionInverse;

uniform ivec2 noiseScale;

in vec2 fragTextureCoordinate;

out float occlusionFactor;

const float RADIUS = 4;
const float MAX_DISTANCE = 2000.0;
const vec2 HALF_2 = vec2(0.5);
const int SAMPLE_COUNT = 64;
const vec3[SAMPLE_COUNT] SAMPLES = vec3[SAMPLE_COUNT](
vec3(0.07142376, 0.040854275, 0.056829352),
vec3(0.042697314, 0.08964906, 0.01356384),
vec3(-0.0656767, -0.017065996, 0.07464502),
vec3(0.046794858, 0.09060632, 3.9542676E-4),
vec3(0.076509066, 0.04823415, 0.0503519),
vec3(-0.026311256, -0.07925761, 0.0644574),
vec3(0.038907114, -0.1002183, 0.009334342),
vec3(0.10686881, 0.021407828, 0.019748494),
vec3(0.01230738, 0.040556073, 0.10589612),
vec3(-0.060474366, -0.05992079, 0.08141673),
vec3(-0.041735157, 0.10318643, 0.04988053),
vec3(-0.033028554, 0.08540611, 0.08740228),
vec3(-0.052442234, -0.1204416, 0.008537516),
vec3(-0.024188636, 0.05850426, 0.12164637),
vec3(0.0134801585, -0.09492563, 0.106185734),
vec3(-0.08863691, -0.0813059, 0.088683195),
vec3(0.10324672, 0.042096093, 0.10946275),
vec3(0.13017984, 0.07962071, 0.058705367),
vec3(0.09504685, -0.12763672, 0.06309884),
vec3(0.06810616, -0.12665993, 0.10712114),
vec3(0.01134698, -0.1293798, 0.13577555),
vec3(-0.048061058, -0.08971123, 0.16855684),
vec3(-0.12617338, -0.060170606, 0.15178646),
vec3(0.03750744, -0.20849474, 0.043368883),
vec3(0.21742153, 0.05194127, 0.03688559),
vec3(-0.14180018, -0.071404174, 0.1764065),
vec3(-0.18176435, 0.1676897, 0.02473084),
vec3(0.037475206, 0.12685467, 0.22404793),
vec3(-0.10478112, 0.22059406, 0.12036503),
vec3(-0.17406002, -0.2121142, 0.07626304),
vec3(0.10916022, 0.19556388, 0.19620448),
vec3(0.21274716, -0.22125651, 0.0510198),
vec3(0.02632687, -0.24426033, 0.2127646),
vec3(0.123885445, -0.3060643, 0.07803497),
vec3(-0.10989856, 0.13378568, 0.30877572),
vec3(0.07760262, 0.352696, 0.07659105),
vec3(0.20609453, -0.17587337, 0.27319995),
vec3(0.25495496, -0.0059493873, 0.309205),
vec3(-0.26405123, 0.2375855, 0.21899074),
vec3(0.35979933, -0.11416962, 0.21457621),
vec3(-0.34627315, -0.14934188, 0.24839601),
vec3(0.15822724, 0.37226054, 0.23808694),
vec3(0.42049024, 0.14050893, 0.20296964),
vec3(0.18043439, 0.3511667, 0.31692138),
vec3(0.4123151, 0.069161616, 0.31819528),
vec3(0.36862242, 0.38610628, 0.109571666),
vec3(0.08989894, -0.53976697, 0.14045873),
vec3(-0.39944163, -0.13553686, 0.40588325),
vec3(0.37147063, -0.3622344, 0.31358388),
vec3(-0.17736939, 0.5181413, 0.30644011),
vec3(-0.50264, -0.4066722, 0.05985422),
vec3(-0.5705339, -0.33418685, 0.11719368),
vec3(0.14656493, 0.61290735, 0.29102314),
vec3(0.5729304, 0.30573052, 0.3044215),
vec3(0.18365106, 0.37387332, 0.61250395),
vec3(0.5545785, 0.523845, 0.05247608),
vec3(0.5334033, 0.26632676, 0.51688546),
vec3(0.08838192, 0.66804904, 0.45641932),
vec3(-0.66909266, -0.36386558, 0.3522877),
vec3(-0.4364715, 0.25892174, 0.7003207),
vec3(0.1331955, 0.8105888, 0.3451286),
vec3(0.29831105, 0.5227373, 0.69264024),
vec3(0.60984784, -0.71165514, 0.11813718),
vec3(-0.6765868, 0.17383541, 0.67600274)
);

// Code from https://medium.com/better-programming/depth-only-ssao-for-forward-renderers-1a3dcfa1873a
vec3 calcViewPosition(vec2 coords) {
    float fragmentDepth = texture(depthTexture, coords).r;

    vec4 ndc = vec4(coords.x * 2.0 - 1.0, coords.y * 2.0 - 1.0, fragmentDepth * 2.0 - 1.0, 1.0);

    vec4 vs_pos = projectionInverse * ndc;
    vs_pos.xyz = vs_pos.xyz / vs_pos.w;

    return vs_pos.xyz;
}

float computeOcclusion() {
    vec3 viewPos = calcViewPosition(fragTextureCoordinate);

    vec3 viewNormal = cross(dFdy(viewPos.xyz), dFdx(viewPos.xyz));

    viewNormal = normalize(viewNormal * -1.0);
    vec3 randomVec = texture(noiseTexture, fragTextureCoordinate * noiseScale).xyz;
    vec3 tangent = normalize(randomVec - viewNormal * dot(randomVec, viewNormal));
    vec3 bitangent = cross(viewNormal, tangent);
    mat3 TBN = mat3(tangent, bitangent, viewNormal);
    float occlusion_factor = 0.0;

    for (int i = 0; i < SAMPLE_COUNT; i++) {
        vec3 samplePos = TBN * SAMPLES[i];

        samplePos = viewPos + samplePos * RADIUS;

        vec4 offset = vec4(samplePos, 1.0);
        offset = projectionMatrix * offset;
        offset.xy /= offset.w;
        offset.xy = offset.xy * HALF_2 + HALF_2;

        float geometryDepth = calcViewPosition(offset.xy).z;
        float rangeCheck = float(abs(viewPos.z - geometryDepth) < RADIUS);
        float distanceScale = max(0.05, 1.0 - smoothstep(0.0, MAX_DISTANCE, min(MAX_DISTANCE, abs(geometryDepth))));

        occlusion_factor += float(geometryDepth >= samplePos.z + 0.0001) * rangeCheck * distanceScale;
    }

    float average_occlusion_factor = occlusion_factor * (1.0 / SAMPLE_COUNT);
    float visibility_factor = 1.0 - average_occlusion_factor;

    visibility_factor = pow(visibility_factor, 5.0);

    return visibility_factor;
}

void main() {
    occlusionFactor = computeOcclusion();
    occlusionFactor = max(occlusionFactor, 0.5);
}