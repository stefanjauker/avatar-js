{
    'includes': {
        'config.gypi',
    },
    'variables': {
        'AVATAR-JS_PATH%': 'out/<(target)/lib.target',
    },
    'target_defaults': {
        'default_configuration': '<(target)',
        'configurations': {
            'Debug': {
                'defines': [ 'DEBUG', '_DEBUG' ],
                'cflags': [ '-g', '-O0' ],
                'conditions': [
                    ['OS == "win"', {
                        'msvs_settings': {
                            'VCLinkerTool': {
                                'GenerateDebugInformation': 'true',
                            },
                        },
                    }],
                    [ 'OS=="linux"', {
                    }],
                    [ 'OS=="mac"', {
                    }],
                ],
            },
            'Release': {
                'defines': [ 'NDEBUG' ],
                'conditions': [
                    ['OS == "win"', {
                    }],
                    [ 'OS=="linux"', {
                    }],
                    [ 'OS=="mac"', {
                    }],
                ],
            },
        }
    },
    'targets': [
        {
            'target_name': 'avatar-js',
            'type': 'shared_library',
            'defines': [
            ],
            'dependencies': [
            ],
            'include_dirs': [
                '<(JAVA_HOME)/include',
            ],
            'conditions': [
                ['OS == "linux"', {
                    'sources': [
                        'os.cpp',
                        'process.cpp',
                        'throw.cpp',
                    ],
                    'libraries': [
                    ],
                    'defines': [
                        '__POSIX__',
                    ],
                    'cflags': [
                        '-fPIC',
                    ],
                    'ldflags': [
                    ],
                    'include_dirs': [
                        '<(JAVA_HOME)/include/linux',
                    ],
                }],
                ['OS == "mac"', {
                    'target_conditions': [
                        ['target_arch=="x64"', {
                            'xcode_settings': {'ARCHS': ['x86_64']},
                        }]
                    ],
                    'sources': [
                        '<(AVATAR-JS_PATH)/../obj.target/avatar-js/os.cpp',
                        '<(AVATAR-JS_PATH)/../obj.target/avatar-js/process.cpp',
                        '<(AVATAR-JS_PATH)/../obj.target/avatar-js/throw.cpp',
                    ],
                    'libraries': [
                    ],
                    'defines': [
                        '__POSIX__',
                    ],
                    'include_dirs': [
                        '<(JAVA_HOME)/include/darwin',
                    ],
                }],
                ['OS == "win"', {
                    'target_conditions': [
                        ['target_arch=="x64"', {
                            'msvs_configuration_platform': 'x64'
                        }]
                    ],
                    'sources': [
                        '<(AVATAR-JS_PATH)/../obj.target/avatar-js/os.cpp',
                        '<(AVATAR-JS_PATH)/../obj.target/avatar-js/process.cpp',
                        '<(AVATAR-JS_PATH)/../obj.target/avatar-js/throw.cpp',
                    ],
                    'defines': [
                        '_WIN32',
                    ],
                    'cflags': [
                    ],
                    'ldflags': [
                    ],
                    'include_dirs': [
                        '<(JAVA_HOME)/include/win32',
                    ],
                    'libraries': [
                    ],
                }]
            ],
        },

    ],

}
