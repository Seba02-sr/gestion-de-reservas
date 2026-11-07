<?php
/*
 * Configuración de phpMyAdmin para auto-login en desarrollo.
 * NO usar en producción.
 */

$cfg['blowfish_secret'] = 'dev-long-secret-please-change-32chars-min-2025';

$i = 1;
$cfg['Servers'][$i]['auth_type'] = 'config';
$cfg['Servers'][$i]['host'] = 'mysql';
$cfg['Servers'][$i]['compress'] = false;
$cfg['Servers'][$i]['AllowNoPassword'] = false;
$cfg['Servers'][$i]['user'] = 'usr_app';
$cfg['Servers'][$i]['password'] = 'usrapp';

