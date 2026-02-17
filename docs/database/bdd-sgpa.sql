-- SGPA full import (structure + constraints + data)
-- Safe re-import on existing database
CREATE DATABASE IF NOT EXISTS `bdd_sgpa` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `bdd_sgpa`;

-- phpMyAdmin SQL Dump
-- version 5.2.3
-- https://www.phpmyadmin.net/
--
-- Hôte : localhost:8889
-- Généré le : lun. 16 fév. 2026 à 18:24
-- Version du serveur : 8.0.44
-- Version de PHP : 8.3.28

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `ligne_vente`;
DROP TABLE IF EXISTS `ligne_commande`;
DROP TABLE IF EXISTS `vente`;
DROP TABLE IF EXISTS `commande`;
DROP TABLE IF EXISTS `app_settings`;
DROP TABLE IF EXISTS `medicament`;
DROP TABLE IF EXISTS `user`;
DROP TABLE IF EXISTS `fournisseur`;
DROP TABLE IF EXISTS `role`;
SET FOREIGN_KEY_CHECKS = 1;


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de données : `bdd_sgpa`
--

-- --------------------------------------------------------

--
-- Structure de la table `app_settings`
--

CREATE TABLE `app_settings` (
  `key` varchar(80) NOT NULL,
  `value` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Déchargement des données de la table `app_settings`
--

INSERT INTO `app_settings` (`key`, `value`) VALUES
('smtp_enabled', 'true'),
('smtp_from', 's.anolden123@gmail.com'),
('smtp_host', 'smtp.gmail.com'),
('smtp_password', ''),
('smtp_port', '587'),
('smtp_username', 's.anolden123@gmail.com'),
('stock_alert_recipients', 's.anolden123@gmail.com');

-- --------------------------------------------------------

--
-- Structure de la table `commande`
--

CREATE TABLE `commande` (
  `id` int NOT NULL,
  `id_fournisseur` int NOT NULL,
  `date_commande` date NOT NULL DEFAULT (curdate()),
  `statut` varchar(20) NOT NULL DEFAULT 'EN_ATTENTE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Déchargement des données de la table `commande`
--

INSERT INTO `commande` (`id`, `id_fournisseur`, `date_commande`, `statut`) VALUES
(1, 1, '2025-10-05', 'RECUE'),
(2, 2, '2025-11-21', 'RECUE'),
(3, 3, '2025-12-28', 'RECUE'),
(4, 4, '2026-01-20', 'RECUE'),
(5, 1, '2026-02-03', 'EN_ATTENTE'),
(6, 2, '2026-02-08', 'EN_ATTENTE'),
(7, 3, '2026-02-15', 'EN_ATTENTE'),
(8, 4, '2026-03-01', 'EN_ATTENTE'),
(9, 1, '2026-02-19', 'EN_ATTENTE'),
(10, 2, '2026-02-26', 'EN_ATTENTE'),
(11, 3, '2026-03-10', 'EN_ATTENTE'),
(12, 4, '2026-03-17', 'EN_ATTENTE');

-- --------------------------------------------------------

--
-- Structure de la table `fournisseur`
--

CREATE TABLE `fournisseur` (
  `id` int NOT NULL,
  `nom` varchar(100) NOT NULL,
  `contact` varchar(100) DEFAULT NULL,
  `adresse` text
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Déchargement des données de la table `fournisseur`
--

INSERT INTO `fournisseur` (`id`, `nom`, `contact`, `adresse`) VALUES
(1, 'Sanofi Distribution', '01 42 00 10 10', '54 Rue La Boetie, 75008 Paris'),
(2, 'Pfizer France', '01 58 07 34 40', '23-25 Av. du Dr Lannelongue, 75014 Paris'),
(3, 'Cooper Pharma', '04 72 69 23 50', 'Place Lucien-Auvert, 77020 Melun'),
(4, 'Alliance Healthcare', '03 20 20 20 20', 'Parc Eurasante, 59320 Loos');

-- --------------------------------------------------------

--
-- Structure de la table `ligne_commande`
--

CREATE TABLE `ligne_commande` (
  `id_commande` int NOT NULL,
  `id_medicament` int NOT NULL,
  `quantite` int NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Déchargement des données de la table `ligne_commande`
--

INSERT INTO `ligne_commande` (`id_commande`, `id_medicament`, `quantite`) VALUES
(1, 2, 40),
(1, 7, 35),
(1, 10, 20),
(2, 1, 120),
(2, 4, 60),
(2, 8, 90),
(3, 5, 70),
(3, 9, 50),
(3, 11, 45),
(4, 3, 50),
(4, 6, 80),
(4, 12, 55),
(5, 2, 30),
(5, 7, 28),
(5, 10, 24),
(6, 4, 40),
(6, 8, 70),
(6, 11, 35),
(7, 1, 100),
(7, 5, 60),
(7, 12, 40),
(8, 3, 45),
(8, 6, 70),
(8, 9, 30),
(9, 2, 36),
(9, 7, 24),
(9, 10, 12),
(10, 4, 50),
(10, 8, 80),
(10, 11, 45),
(11, 1, 90),
(11, 5, 55),
(11, 12, 42),
(12, 3, 48),
(12, 6, 65),
(12, 9, 34);

-- --------------------------------------------------------

--
-- Structure de la table `ligne_vente`
--

CREATE TABLE `ligne_vente` (
  `id_vente` int NOT NULL,
  `id_medicament` int NOT NULL,
  `quantite` int NOT NULL,
  `prix_unitaire_ht` decimal(10,2) NOT NULL,
  `cout_achat_unitaire_ht` decimal(10,2) NOT NULL DEFAULT '0.00',
  `taux_tva` decimal(5,2) NOT NULL,
  `montant_tva` decimal(10,2) NOT NULL,
  `prix_unitaire_ttc` decimal(10,2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Déchargement des données de la table `ligne_vente`
--

INSERT INTO `ligne_vente` (`id_vente`, `id_medicament`, `quantite`, `prix_unitaire_ht`, `cout_achat_unitaire_ht`, `taux_tva`, `montant_tva`, `prix_unitaire_ttc`) VALUES
(1, 1, 12, 2.20, 1.50, 2.10, 0.05, 2.25),
(1, 4, 6, 4.30, 2.92, 10.00, 0.43, 4.73),
(1, 8, 5, 2.80, 1.90, 20.00, 0.56, 3.36),
(2, 2, 4, 7.80, 5.30, 2.10, 0.16, 7.96),
(2, 3, 3, 9.40, 6.39, 2.10, 0.20, 9.60),
(2, 6, 8, 3.60, 2.45, 2.10, 0.08, 3.68),
(3, 1, 14, 2.20, 1.50, 2.10, 0.05, 2.25),
(3, 5, 7, 5.10, 3.47, 10.00, 0.51, 5.61),
(3, 9, 4, 7.20, 4.90, 20.00, 1.44, 8.64),
(4, 2, 3, 7.80, 5.30, 2.10, 0.16, 7.96),
(4, 7, 5, 6.90, 4.69, 2.10, 0.14, 7.04),
(4, 8, 9, 2.80, 1.90, 20.00, 0.56, 3.36),
(5, 3, 6, 9.40, 6.39, 2.10, 0.20, 9.60),
(5, 4, 8, 4.30, 2.92, 10.00, 0.43, 4.73),
(5, 6, 10, 3.60, 2.45, 2.10, 0.08, 3.68),
(6, 1, 16, 2.20, 1.50, 2.10, 0.05, 2.25),
(6, 5, 8, 5.10, 3.47, 10.00, 0.51, 5.61),
(6, 9, 5, 7.20, 4.90, 20.00, 1.44, 8.64),
(7, 2, 4, 7.80, 5.30, 2.10, 0.16, 7.96),
(7, 8, 10, 2.80, 1.90, 20.00, 0.56, 3.36),
(7, 10, 2, 12.50, 8.50, 2.10, 0.26, 12.76),
(8, 3, 7, 9.40, 6.39, 2.10, 0.20, 9.60),
(8, 4, 9, 4.30, 2.92, 10.00, 0.43, 4.73),
(8, 6, 11, 3.60, 2.45, 2.10, 0.08, 3.68),
(9, 1, 18, 2.20, 1.50, 2.10, 0.05, 2.25),
(9, 5, 9, 5.10, 3.47, 10.00, 0.51, 5.61),
(9, 9, 6, 7.20, 4.90, 20.00, 1.44, 8.64),
(10, 2, 5, 7.80, 5.30, 2.10, 0.16, 7.96),
(10, 7, 6, 6.90, 4.69, 2.10, 0.14, 7.04),
(10, 8, 11, 2.80, 1.90, 20.00, 0.56, 3.36),
(11, 3, 8, 9.40, 6.39, 2.10, 0.20, 9.60),
(11, 4, 10, 4.30, 2.92, 10.00, 0.43, 4.73),
(11, 6, 12, 3.60, 2.45, 2.10, 0.08, 3.68),
(12, 1, 10, 2.20, 1.50, 2.10, 0.05, 2.25),
(12, 5, 5, 5.10, 3.47, 10.00, 0.51, 5.61),
(12, 9, 3, 7.20, 4.90, 20.00, 1.44, 8.64),
(13, 2, 3, 7.80, 5.30, 2.10, 0.16, 7.96),
(13, 7, 4, 6.90, 4.69, 2.10, 0.14, 7.04),
(13, 8, 6, 2.80, 1.90, 20.00, 0.56, 3.36),
(14, 1, 8, 2.20, 1.50, 2.10, 0.05, 2.25),
(14, 4, 4, 4.30, 2.92, 10.00, 0.43, 4.73),
(14, 10, 1, 12.50, 8.50, 2.10, 0.26, 12.76),
(15, 3, 5, 9.40, 6.39, 2.10, 0.20, 9.60),
(15, 5, 4, 5.10, 3.47, 10.00, 0.51, 5.61),
(15, 8, 5, 2.80, 1.90, 20.00, 0.56, 3.36),
(16, 1, 7, 2.20, 1.50, 2.10, 0.05, 2.25),
(16, 6, 6, 3.60, 2.45, 2.10, 0.08, 3.68),
(16, 9, 2, 7.20, 4.90, 20.00, 1.44, 8.64),
(17, 2, 2, 7.80, 5.30, 2.10, 0.16, 7.96),
(17, 4, 5, 4.30, 2.92, 10.00, 0.43, 4.73),
(17, 7, 3, 6.90, 4.69, 2.10, 0.14, 7.04),
(18, 3, 4, 9.40, 6.39, 2.10, 0.20, 9.60),
(18, 5, 3, 5.10, 3.47, 10.00, 0.51, 5.61),
(18, 8, 4, 2.80, 1.90, 20.00, 0.56, 3.36),
(19, 1, 6, 2.20, 1.50, 2.10, 0.05, 2.25),
(19, 6, 5, 3.60, 2.45, 2.10, 0.08, 3.68),
(19, 9, 2, 7.20, 4.90, 20.00, 1.44, 8.64),
(20, 1, 5, 2.20, 1.50, 2.10, 0.05, 2.25),
(20, 5, 2, 5.10, 3.47, 10.00, 0.51, 5.61),
(20, 8, 3, 2.80, 1.90, 20.00, 0.56, 3.36),
(23, 13, 20, 2.20, 1.50, 2.10, 0.05, 2.25),
(23, 14, 6, 7.80, 5.30, 2.10, 0.16, 7.96),
(24, 5, 30, 5.10, 3.47, 10.00, 0.51, 5.61),
(26, 13, 2, 2.20, 1.50, 2.10, 0.05, 2.25),
(27, 1, 17, 2.20, 1.50, 2.10, 0.05, 2.25),
(27, 2, 8, 7.80, 5.30, 2.10, 0.16, 7.96),
(27, 13, 3, 2.20, 1.50, 2.10, 0.05, 2.25),
(28, 1, 9, 2.20, 1.50, 2.10, 0.05, 2.25),
(28, 4, 4, 4.30, 2.92, 10.00, 0.43, 4.73),
(28, 8, 6, 2.80, 1.90, 20.00, 0.56, 3.36),
(29, 2, 3, 7.80, 5.30, 2.10, 0.16, 7.96),
(29, 7, 2, 6.90, 4.69, 2.10, 0.14, 7.04),
(29, 10, 1, 12.50, 8.50, 2.10, 0.26, 12.76),
(30, 3, 2, 9.40, 6.39, 2.10, 0.20, 9.60),
(30, 5, 4, 5.10, 3.47, 10.00, 0.51, 5.61),
(30, 9, 1, 7.20, 4.90, 20.00, 1.44, 8.64),
(31, 6, 12, 3.60, 2.45, 2.10, 0.08, 3.68),
(31, 1, 5, 2.20, 1.50, 2.10, 0.05, 2.25),
(31, 8, 10, 2.80, 1.90, 20.00, 0.56, 3.36),
(32, 14, 2, 7.80, 5.30, 2.10, 0.16, 7.96),
(32, 4, 6, 4.30, 2.92, 10.00, 0.43, 4.73),
(32, 5, 3, 5.10, 3.47, 10.00, 0.51, 5.61),
(33, 13, 7, 2.20, 1.50, 2.10, 0.05, 2.25),
(33, 3, 3, 9.40, 6.39, 2.10, 0.20, 9.60),
(33, 9, 2, 7.20, 4.90, 20.00, 1.44, 8.64),
(34, 2, 4, 7.80, 5.30, 2.10, 0.16, 7.96),
(34, 6, 8, 3.60, 2.45, 2.10, 0.08, 3.68),
(34, 8, 6, 2.80, 1.90, 20.00, 0.56, 3.36),
(35, 1, 10, 2.20, 1.50, 2.10, 0.05, 2.25),
(35, 5, 5, 5.10, 3.47, 10.00, 0.51, 5.61),
(35, 10, 2, 12.50, 8.50, 2.10, 0.26, 12.76),
(36, 4, 7, 4.30, 2.92, 10.00, 0.43, 4.73),
(36, 7, 4, 6.90, 4.69, 2.10, 0.14, 7.04),
(36, 8, 8, 2.80, 1.90, 20.00, 0.56, 3.36);

-- --------------------------------------------------------

--
-- Structure de la table `medicament`
--

CREATE TABLE `medicament` (
  `id` int NOT NULL,
  `nom_commercial` varchar(100) NOT NULL,
  `principe_actif` varchar(100) NOT NULL,
  `forme_galenique` varchar(50) NOT NULL,
  `dosage` varchar(50) NOT NULL,
  `prix_public` decimal(10,2) NOT NULL,
  `prix_achat_ht` decimal(10,2) NOT NULL DEFAULT '0.00',
  `taux_tva` decimal(5,2) NOT NULL DEFAULT '20.00',
  `necessite_ordonnance` tinyint(1) NOT NULL DEFAULT '0',
  `date_peremption` date NOT NULL,
  `stock_actuel` int NOT NULL DEFAULT '0',
  `stock_minimum` int NOT NULL DEFAULT '5'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Déchargement des données de la table `medicament`
--

INSERT INTO `medicament` (`id`, `nom_commercial`, `principe_actif`, `forme_galenique`, `dosage`, `prix_public`, `prix_achat_ht`, `taux_tva`, `necessite_ordonnance`, `date_peremption`, `stock_actuel`, `stock_minimum`) VALUES
(1, 'Doliprane', 'Paracetamol', 'Comprime', '1000 mg', 2.20, 1.50, 2.10, 0, '2028-12-15', 103, 40),
(2, 'Amoxicilline Biogaran', 'Amoxicilline', 'Gelule', '500 mg', 7.80, 5.30, 2.10, 1, '2026-04-20', 0, 15),
(3, 'Ventoline', 'Salbutamol', 'Inhalateur', '100 mcg', 9.40, 6.39, 2.10, 1, '2027-07-30', 26, 10),
(4, 'Smecta', 'Diosmectite', 'Sachet', '3 g', 4.30, 2.92, 10.00, 0, '2026-05-01', 12, 12),
(5, 'Gaviscon', 'Alginate de sodium', 'Suspension buvable', '250 ml', 5.10, 3.47, 10.00, 0, '2027-09-18', 5, 15),
(6, 'Ibuprofene EG', 'Ibuprofene', 'Comprime', '400 mg', 3.60, 2.45, 2.10, 0, '2028-03-11', 60, 20),
(7, 'Levothyrox', 'Levothyroxine sodique', 'Comprime', '75 mcg', 6.90, 4.69, 2.10, 1, '2026-03-25', 9, 18),
(8, 'Serum Physiologique', 'Chlorure de sodium', 'Unidose', '5 ml', 2.80, 1.90, 20.00, 0, '2026-11-05', 50, 25),
(9, 'Biafine', 'Trolamine', 'Creme', '93 g', 7.20, 4.90, 20.00, 0, '2027-12-01', 2, 10),
(10, 'Xanax', 'Alprazolam', 'Comprime', '0.25 mg', 12.50, 8.50, 2.10, 1, '2026-06-30', 5, 10),
(11, 'Aerius', 'Desloratadine', 'Comprime', '5 mg', 4.90, 3.33, 2.10, 0, '2027-04-12', 42, 15),
(12, 'Fervex', 'Paracetamol + Vitamine C', 'Sachet', '8 sachets', 6.30, 4.28, 10.00, 0, '2026-10-22', 28, 12),
(13, 'Doliprane', 'Paracetamol', 'Comprime', '1000 mg', 2.20, 1.50, 2.10, 0, '2026-06-20', 0, 10),
(14, 'Amoxicilline Biogaran', 'Amoxicilline', 'Gelule', '500 mg', 7.80, 5.30, 2.10, 1, '2026-03-10', 0, 8),
(15, 'Ventoline', 'Salbutamol', 'Inhalateur', '100 mcg', 9.40, 6.39, 2.10, 1, '2026-08-15', 4, 6),
(16, 'Smecta', 'Diosmectite', 'Sachet', '3 g', 4.30, 2.92, 10.00, 0, '2026-02-20', 5, 8);

-- --------------------------------------------------------

--
-- Structure de la table `role`
--

CREATE TABLE `role` (
  `id` int NOT NULL,
  `libelle` varchar(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Déchargement des données de la table `role`
--

INSERT INTO `role` (`id`, `libelle`) VALUES
(1, 'Pharmacien'),
(2, 'Preparateur/Vendeur');

-- --------------------------------------------------------

--
-- Structure de la table `user`
--

CREATE TABLE `user` (
  `id` int NOT NULL,
  `nom` varchar(50) NOT NULL,
  `prenom` varchar(50) NOT NULL,
  `email` varchar(100) NOT NULL,
  `password` varchar(255) NOT NULL,
  `id_role` int NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Déchargement des données de la table `user`
--

INSERT INTO `user` (`id`, `nom`, `prenom`, `email`, `password`, `id_role`) VALUES
(1, 'Admin', 'Pharmacien', 'admin@pharmacie.fr', 'PBKDF2$65536$TYn77wDWD5HXB9Hv14Mbtg==$Q3sLfCB1nh19idcOzmrMnuy9MFA4Fs24nskI+PLx2jA=', 1),
(2, 'Vendeur', 'Jean', 'jean@pharmacie.fr', 'PBKDF2$65536$tSKQ74f9LVhZtDONbLAwbg==$B9MU4uF8Qza5If5jVSFcvumhUzWWPQl3W205WV782e4=', 2),
(3, 'Vendeuse', 'Clara', 'clara@pharmacie.fr', 'PBKDF2$65536$mlR213JfMDMf1/7T94rbFA==$owJ8ByNTVgLHNgpyuRZoRCMv7OP5lzVe37bJgvQ3s1k=', 2);

-- --------------------------------------------------------

--
-- Structure de la table `vente`
--

CREATE TABLE `vente` (
  `id` int NOT NULL,
  `date_heure` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `montant_ht` decimal(10,2) NOT NULL,
  `montant_tva` decimal(10,2) NOT NULL,
  `montant_ttc` decimal(10,2) NOT NULL,
  `sur_ordonnance` tinyint(1) NOT NULL DEFAULT '0',
  `id_user` int NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Déchargement des données de la table `vente`
--

INSERT INTO `vente` (`id`, `date_heure`, `montant_ht`, `montant_tva`, `montant_ttc`, `sur_ordonnance`, `id_user`) VALUES
(1, '2025-03-10 10:12:00', 66.20, 5.98, 72.18, 0, 2),
(2, '2025-04-12 16:20:00', 88.20, 1.88, 90.08, 1, 1),
(3, '2025-05-15 11:05:00', 95.30, 10.03, 105.33, 0, 3),
(4, '2025-06-17 18:14:00', 83.10, 6.22, 89.32, 1, 1),
(5, '2025-07-12 09:48:00', 126.80, 5.44, 132.24, 1, 2),
(6, '2025-08-14 14:33:00', 112.00, 12.08, 124.08, 0, 3),
(7, '2025-09-16 10:27:00', 84.20, 6.76, 90.96, 1, 1),
(8, '2025-10-18 17:52:00', 144.10, 6.15, 150.25, 1, 2),
(9, '2025-11-14 12:40:00', 128.70, 14.13, 142.83, 0, 3),
(10, '2025-12-16 15:16:00', 111.20, 7.80, 119.00, 1, 1),
(11, '2026-01-15 13:02:00', 161.40, 6.86, 168.26, 1, 2),
(12, '2026-02-01 10:08:00', 69.10, 7.37, 76.47, 0, 3),
(13, '2026-02-05 16:44:00', 67.80, 4.40, 72.20, 1, 1),
(14, '2026-02-09 09:20:00', 47.30, 2.38, 49.68, 1, 2),
(15, '2026-02-10 11:05:00', 81.40, 5.84, 87.24, 1, 3),
(16, '2026-02-11 14:18:00', 51.40, 3.71, 55.11, 0, 2),
(17, '2026-02-12 15:30:00', 57.80, 2.89, 60.69, 1, 1),
(18, '2026-02-13 10:40:00', 64.10, 4.57, 68.67, 1, 3),
(19, '2026-02-14 12:10:00', 45.60, 3.58, 49.18, 0, 2),
(20, '2026-03-03 09:55:00', 29.60, 2.95, 32.55, 0, 1),
(21, '2026-02-09 22:56:30', 12.00, 2.40, 14.40, 0, 1),
(22, '2026-02-09 23:04:35', 12.00, 2.40, 14.40, 1, 1),
(23, '2026-02-09 23:07:00', 98.80, 3.51, 102.31, 1, 1),
(24, '2026-02-09 23:09:01', 153.00, 15.30, 168.30, 0, 1),
(26, '2026-02-10 09:44:29', 4.40, 0.09, 4.49, 1, 1),
(27, '2026-02-10 19:54:00', 106.40, 2.23, 108.63, 1, 1),
(28, '2026-02-16 10:20:00', 53.80, 5.53, 59.33, 0, 2),
(29, '2026-02-17 11:45:00', 49.70, 1.02, 50.72, 1, 3),
(30, '2026-02-18 15:10:00', 46.40, 3.88, 50.28, 1, 2),
(31, '2026-02-20 09:35:00', 82.20, 6.81, 89.01, 0, 3),
(32, '2026-02-22 16:25:00', 56.70, 4.43, 61.13, 1, 1),
(33, '2026-03-02 10:55:00', 58.00, 3.83, 61.83, 1, 2),
(34, '2026-03-09 14:12:00', 76.80, 4.64, 81.44, 1, 3),
(35, '2026-03-16 17:40:00', 72.50, 3.57, 76.07, 1, 2),
(36, '2026-03-23 11:05:00', 80.10, 8.05, 88.15, 1, 1);

--
-- Index pour les tables déchargées
--

--
-- Index pour la table `app_settings`
--
ALTER TABLE `app_settings`
  ADD PRIMARY KEY (`key`);

--
-- Index pour la table `commande`
--
ALTER TABLE `commande`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_commande_fournisseur` (`id_fournisseur`);

--
-- Index pour la table `fournisseur`
--
ALTER TABLE `fournisseur`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `ligne_commande`
--
ALTER TABLE `ligne_commande`
  ADD PRIMARY KEY (`id_commande`,`id_medicament`),
  ADD KEY `fk_ligne_commande_medicament` (`id_medicament`);

--
-- Index pour la table `ligne_vente`
--
ALTER TABLE `ligne_vente`
  ADD PRIMARY KEY (`id_vente`,`id_medicament`),
  ADD KEY `fk_ligne_vente_medicament` (`id_medicament`);

--
-- Index pour la table `medicament`
--
ALTER TABLE `medicament`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `role`
--
ALTER TABLE `role`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `user`
--
ALTER TABLE `user`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `email` (`email`),
  ADD KEY `fk_user_role` (`id_role`);

--
-- Index pour la table `vente`
--
ALTER TABLE `vente`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_vente_user` (`id_user`);

--
-- AUTO_INCREMENT pour les tables déchargées
--

--
-- AUTO_INCREMENT pour la table `commande`
--
ALTER TABLE `commande`
  MODIFY `id` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=13;

--
-- AUTO_INCREMENT pour la table `fournisseur`
--
ALTER TABLE `fournisseur`
  MODIFY `id` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT pour la table `medicament`
--
ALTER TABLE `medicament`
  MODIFY `id` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=18;

--
-- AUTO_INCREMENT pour la table `role`
--
ALTER TABLE `role`
  MODIFY `id` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT pour la table `user`
--
ALTER TABLE `user`
  MODIFY `id` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT pour la table `vente`
--
ALTER TABLE `vente`
  MODIFY `id` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=37;

--
-- Contraintes pour les tables déchargées
--

--
-- Contraintes pour la table `commande`
--
ALTER TABLE `commande`
  ADD CONSTRAINT `fk_commande_fournisseur` FOREIGN KEY (`id_fournisseur`) REFERENCES `fournisseur` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `ligne_commande`
--
ALTER TABLE `ligne_commande`
  ADD CONSTRAINT `fk_ligne_commande_commande` FOREIGN KEY (`id_commande`) REFERENCES `commande` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_ligne_commande_medicament` FOREIGN KEY (`id_medicament`) REFERENCES `medicament` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `ligne_vente`
--
ALTER TABLE `ligne_vente`
  ADD CONSTRAINT `fk_ligne_vente_medicament` FOREIGN KEY (`id_medicament`) REFERENCES `medicament` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_ligne_vente_vente` FOREIGN KEY (`id_vente`) REFERENCES `vente` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `user`
--
ALTER TABLE `user`
  ADD CONSTRAINT `fk_user_role` FOREIGN KEY (`id_role`) REFERENCES `role` (`id`);

--
-- Contraintes pour la table `vente`
--
ALTER TABLE `vente`
  ADD CONSTRAINT `fk_vente_user` FOREIGN KEY (`id_user`) REFERENCES `user` (`id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
