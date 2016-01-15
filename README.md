FORMAT: 1A
HOST: http://dgtl.pro

# Authorization

Авторизация [![Build Status](https://travis-ci.org/unicorndevru/auth.svg?branch=master)](https://travis-ci.org/unicorndevru/auth)

# Group Auth

## Auth [/api/auth]

Контейнер авторизации.

Теперь чтобы стать админом локально достаточно установить переменную среды `ADMINS_ALL` и присвоить ей значение `true`
На православных маках это делается примерно так `export ADMINS_ALL=true`

Теперь AuthStatus возвращает и список Identities пользователя. У identity поля email и isEmailVerified опциональные.

+ Model

    + Headers

            Content-Type: application/json

    + Body

            {
                "meta": {
                    "href":"/api/auth"
                },
                "user":"user_reference",
                "roles":["role1","role2"],
                "isSwitched":false
                "identities": {
                    "meta": {
                        "mediaType" : "sometypejson+1andother",
                        "href": "/api/auth/identities"
                    }
            }

### Get Status [GET]

+ Response 200
    [Auth][]
    
### Authorize [POST]

Для социальных провайдеров значение provider может быть `vk` или `google` или `facebook`.
Теперь поддерживаем все три варианта. Токен само собой токен от провайдера.

Для авторизации по Email провайдер должен быть только `email` и объект запроса
должен иметь поля `email` и `password`
    
+ Request

    + Headers

            Content-Type: application/json

    + Body

            {
                "provider":"facebook",
                "token":"some_token"               
            }
            
+ Request

    + Headers

            Content-Type: application/json

    + Body

            {
                "provider":"email",
                "email":"some@email.me",
                "password":"passw"
            }            

+ Response 200
    [Auth][]

    

### Register email's user [PUT]

+ Request

    + Headers

            Content-Type: application/json

    + Body

            {
                "provider":"email",
                "email":"some@email.me",
                "password":"passw"
            }        
            
+ Response 201
        [Auth][]
        
# Group Actions

## Change Password [/api/auth/actions/changePassword] 

Контейнер

### Change Password [POST]

Поле oldPass опционально. Т.к. у социальных юзеров его не может быть.

+ Request 

    + Headers
    
            Content-Type: application/json
            
    + Body
    
            {
                "oldPass":"some_old_password" ,
                "newPass":"some_new_password"
            }
+ Response 200
        [Auth][]
      
## Recover Password Start [/api/auth/actions/startPasswordRecovery] 

Контейнер

### Recover Password Start [POST]

Инициирует процедуру восстановления пароля.
Отсылает секретный хэш пользователю с данным email.

+ Request 

    + Headers
    
            Content-Type: application/json
            
    + Body
    
            {
                "email":"some@fuckin.ru" 
            }
+ Response 204
        
## Recover Password Token Check [/api/auth/actions/checkPasswordRecovery] 

Метод для проверки валидности токена. Если токен валиден 204 иначе 401.

### Recover Password Token Check [POST]

+ Request 

    + Headers
    
            Content-Type: application/json
            
    + Body
    
            {
                "token":"asdasdasdasdasdtokenasdasdasdasdasd" 
            }
+ Response 204        

## Recover Set New Password [/api/auth/actions/recoverPassword] 

Метод который уже устанавливает новый пароль.

### Recover Set New Password [POST]

+ Request 

    + Headers
    
            Content-Type: application/json
            
    + Body
    
            {
                "token":"asdasdasdasdasdtokenasdasdasdasdasd",
                "newPass":"password"
            }
+ Response 200
        [Auth][]
        
## Check email availability [/api/auth/actions/checkEmailAvailability] 
        
Метод для проверки доступности email для регистрации. Если недоступен то 400.
        
### Check email availability [POST]
        
+ Request 
    
    + Headers
          
        Content-Type: application/json
                    
    + Body
           
        {
            "email":"email@test.ru"
                        
        }
+ Response 204

## Verify email [/api/auth/actions/verifyEmail] 
        
Метод для подтверждения email.
        
### Verify email [POST]
        
+ Request 
    
    + Headers
          
        Content-Type: application/json
                    
    + Body
           
        {
            "token":"some_secret_token_from_email"
                        
        }
+ Response 204

## Manual Verify Email Request [/api/auth/actions/requestEmailVerify] 
        
Метод для ручного запуска подтверждения email.

Пользователь должен быть залогинен.
        
### Manual Verify Email Request [POST]
    
+ Response 204

## Start Email Change Request [/api/auth/actions/startEmailChange]

Метод для начала процедуры смена почты пользователя. Пользователь должен быть залогинен.

### Start Email Change Request [POST]
+ Request 
    
    + Headers
          
        Content-Type: application/json
                    
    + Body
           
        {
            "email":"new_valid_email@mail.sru"
                        
        }
+ Response 204

## Finish Email Change  [/api/auth/actions/finishEmailChange] 

Метод который и производит смену почты у пользователя

### Finish Email Change [POST]

+ Request 

    + Headers
    
            Content-Type: application/json
            
    + Body
    
            {
                "token":"token_from_email" 
            }
+ Response 204   
   
## Become another user [/api/auth/actions/switch]

### Become another user [POST]

Метод для того, чтобы стать "другим" юзером.

Ставит в `/api/auth` флаг `isSwitched: true` 
    
+ Request 
        
    + Headers
        
        Content-Type: application/json
        
    + Body
        
        {
         "user": {
            "meta": {
                "href" :"/api/users/plainmeta_of_user_to_become"
            }
         }
        }
     
+ Response 200
        [Auth][]        

### Unbecome another user [DELETE]

Метод для того чтобы перестать быть "другим" юзером.
Удаляет все данные второго пользователся из текущий сессии.    
    
+ Response 200

# Group Identities

## Get IdentitiesList For Current User [/api/auth/identities]

Получение списка всех Identity для текущего пользователя.

### Get IdentitiesList For Current User [GET]

+ Response 200
    
    + Headers
    
                Content-Type: application/json
    
    + Body
    
               {
                "meta": {
                         "href": "/api/auth/identities"
                        },
                        
                "items": [
                    {
                     "meta": {
                              "href": "/api/auth/identities/555ccc6bsddsjjs7"
                             },
                     "id": "somecorrectid",
                     "identityId" : {
                                    "providerId":"email",
                                    "userId":"some@fuckingmail.com"
                                    },
                     "email" : "some@fuckingmail.com",
                     "isEmailVerified" : false
                     }
                ]
                
               }

## Get Identity [/api/auth/identities/{identityId}]

Метод получения Identity по составному Id.
Поля email и isEmailVerified опциональны у Identity.

### Get Identity [GET]

+ Parameters
    
    + identityId: `somecorrectidentityid` (required, string) - Id конкретной Identity.
    
+ Response 200
    
    + Headers
    
                Content-Type: application/json
    
    + Body
    
               {
                "meta": {
                         "href": "/api/auth/identities/providerName/userId"
                        },
                "id": "somecorrectid",
                "identityId" : {
                        "providerId":"email",
                        "userId":"some@fuckingmail.com"
                       },
                "email" : "some@fuckingmail.com",
                "isEmailVerified" : false
               }
         
